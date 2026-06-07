package domains.hack.table.hack

import cats.effect.IO
import domains.hack.objects.HackId
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.{SubmissionId, SubmissionLanguage}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import domains.user.objects.Username
import domains.hack.table.hack.HackTableSupport.*
import io.circe.parser.decode
import judgeprotocol.objects.response.{JudgeResult, JudgeTaskFileRef}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID
import scala.util.matching.Regex

object HackJudgeTable:

  final case class ClaimedHackAttemptRecord(
    hackId: HackId,
    targetSubmission: ClaimedSubmission,
    authorUsername: Username,
    subtaskIndex: Int,
    input: String,
    strategyProviderSource: Option[String],
    oldResult: JudgeResult
  )

  final case class ProblemHackTestcaseRecord(
    hackId: HackId,
    subtaskIndex: Int,
    inputRef: JudgeTaskFileRef,
    answerRef: Option[JudgeTaskFileRef]
  )

  private def claimNextSQL(languageCount: Int): String =
    val placeholders = List.fill(languageCount)("?").mkString(", ")
    val languagePredicate =
      if languageCount == 0 then "program.data ->> 'language' = 'text'"
      else s"(program.data ->> 'language' = 'text' or program.data ->> 'language' in ($placeholders))"
    s"""
      |with next_hack as (
      |  select h.id
      |  from hack_attempts h
      |  join submissions target on target.public_id = h.target_submission_public_id
      |  join problems p on p.id = h.problem_id
      |  where h.status = 'queued'
      |    and target.status = 'completed'
      |    and target.judge_result is not null
      |    and p.ready = true
      |    and exists (
      |      select 1
      |      from jsonb_each(target.program_manifest -> 'programs') as program(role, data)
      |      where $languagePredicate
      |    )
      |  order by h.created_at asc, h.public_id asc
      |  for update skip locked
      |  limit 1
      |)
      |update hack_attempts h
      |set status = 'running',
      |    started_at = ?
      |from next_hack nh
      |join submissions target on true
      |join problems p on true
      |where h.id = nh.id
      |  and target.public_id = h.target_submission_public_id
      |  and p.id = h.problem_id
      |returning h.public_id,
      |          h.author_username,
      |          h.subtask_index,
      |          h.input_text,
      |          h.strategy_provider_source,
      |          target.public_id as target_submission_public_id,
      |          target.problem_id as target_problem_id,
      |          p.slug as problem_slug,
      |          target.program_manifest::text as program_manifest,
      |          target.judge_result::text as old_result
      |""".stripMargin

  def claimNextForLanguages(
    connection: Connection,
    languages: List[SubmissionLanguage],
    startedAt: Instant
  ): IO[Option[ClaimedHackAttemptRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(claimNextSQL(languages.size))
      try
        languages.zipWithIndex.foreach { case (language, index) =>
          statement.setString(index + 1, encodeSubmissionLanguage(language))
        }
        statement.setTimestamp(languages.size + 1, Timestamp.from(startedAt))
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readClaimedHackAttempt(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private val listProblemHackTestcasesSQL: String =
    """
      |select hack_attempt_public_id, subtask_index, input_text, answer_text
      |from problem_hack_testcases
      |where problem_id = ?
      |order by created_at asc, hack_attempt_public_id asc
      |""".stripMargin

  def listProblemHackTestcases(connection: Connection, problemId: ProblemId): IO[List[ProblemHackTestcaseRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(listProblemHackTestcasesSQL)
      try
        statement.setObject(1, problemId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readProblemHackTestcase(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val HackPathPattern: Regex = """^__hack__/([1-9][0-9]*)\.(in|ans)$""".r

  private val readHackDataByPathSQL: String =
    """
      |select pht.input_text, pht.answer_text
      |from problem_hack_testcases pht
      |join problems p on p.id = pht.problem_id
      |where p.slug = ?
      |  and pht.hack_attempt_public_id = ?
      |""".stripMargin

  def readHackDataByPath(connection: Connection, problemSlug: ProblemSlug, rawPath: String): IO[Option[(String, Array[Byte])]] =
    rawPath match
      case HackPathPattern(rawHackId, extension) =>
        IO.blocking {
          val statement = connection.prepareStatement(readHackDataByPathSQL)
          try
            statement.setString(1, problemSlug.value)
            statement.setLong(2, rawHackId.toLong)
            val resultSet = statement.executeQuery()
            try
              if resultSet.next() then
                val content =
                  if extension == "in" then Some(resultSet.getString("input_text"))
                  else Option(resultSet.getString("answer_text"))
                content.map(value => rawPath.split('/').last -> value.getBytes(StandardCharsets.UTF_8))
              else None
            finally resultSet.close()
          finally statement.close()
        }
      case _ =>
        IO.pure(None)

  private def readClaimedHackAttempt(resultSet: ResultSet): ClaimedHackAttemptRecord =
    val targetSubmission =
      ClaimedSubmission(
        id = SubmissionId(resultSet.getLong("target_submission_public_id")),
        problemId = ProblemId(resultSet.getObject("target_problem_id", classOf[UUID])),
        problemSlug = ProblemSlug(resultSet.getString("problem_slug")),
        programManifest = readProgramManifest(resultSet.getString("program_manifest"))
      )
    ClaimedHackAttemptRecord(
      hackId = HackId(resultSet.getLong("public_id")),
      targetSubmission = targetSubmission,
      authorUsername = Username.canonical(resultSet.getString("author_username")),
      subtaskIndex = resultSet.getInt("subtask_index"),
      input = resultSet.getString("input_text"),
      strategyProviderSource = Option(resultSet.getString("strategy_provider_source")),
      oldResult = decode[JudgeResult](resultSet.getString("old_result"))
        .fold(error => throw IllegalStateException(s"Invalid target judge result JSON: ${error.getMessage}"), identity)
    )

  private def readProblemHackTestcase(resultSet: ResultSet): ProblemHackTestcaseRecord =
    val hackId = HackId(resultSet.getLong("hack_attempt_public_id"))
    val input = resultSet.getString("input_text").getBytes(StandardCharsets.UTF_8)
    val answer = Option(resultSet.getString("answer_text")).map(_.getBytes(StandardCharsets.UTF_8))
    ProblemHackTestcaseRecord(
      hackId = hackId,
      subtaskIndex = resultSet.getInt("subtask_index"),
      inputRef = fileRef(inputPath(hackId), input),
      answerRef = answer.map(bytes => fileRef(answerPath(hackId), bytes))
    )

  def inputPath(hackId: HackId): String =
    s"__hack__/${hackId.value}.in"

  def answerPath(hackId: HackId): String =
    s"__hack__/${hackId.value}.ans"

  private def fileRef(path: String, bytes: Array[Byte]): JudgeTaskFileRef =
    JudgeTaskFileRef.unsafe(path, bytes.length.toLong, sha256Hex(bytes))

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  private def readProgramManifest(raw: String): SubmissionProgramManifest =
    decode[SubmissionProgramManifest](raw)
      .fold(error => throw IllegalStateException(s"Invalid submission program manifest JSON: ${error.getMessage}"), identity)

  private def encodeSubmissionLanguage(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"
      case SubmissionLanguage.Text => "text"
