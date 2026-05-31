package domains.submission.table.submission

import cats.effect.IO
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionSourceCode}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeState}
import domains.submission.table.submission.SubmissionTableSupport.*

import java.sql.Connection

object SubmissionJudgeTable:

  private def claimNextForLanguagesSQL(languageCount: Int): String =
    val placeholders = List.fill(languageCount)("?").mkString(", ")
    s"""
      |with next_submission as (
      |  select s.id
      |  from submissions s
      |  join problems p on p.id = s.problem_id
      |  where s.status = 'queued'
      |    and s.language in ($placeholders)
      |    and p.ready = true
      |  order by s.submitted_at asc, s.public_id asc
      |  for update skip locked
      |  limit 1
      |)
      |update submissions s
      |set status = ?,
      |    started_at = ?,
      |    finished_at = ?,
      |    verdict = ?,
      |    judge_message = ?,
      |    time_used_ms = null,
      |    memory_used_kb = null,
      |    score = null,
      |    judge_result = null
      |from next_submission ns, problems p
      |where s.id = ns.id
      |  and p.id = s.problem_id
      |returning s.public_id, s.problem_id, p.slug as problem_slug, s.language, s.source_code
      |""".stripMargin

  def claimNextForLanguages(
    connection: Connection,
    languages: List[SubmissionLanguage],
    runningState: SubmissionJudgeState
  ): IO[Option[ClaimedSubmission]] =
    if languages.isEmpty then IO.pure(None)
    else IO.blocking {
      val statement = connection.prepareStatement(claimNextForLanguagesSQL(languages.size))
      try
        languages.zipWithIndex.foreach { case (language, index) =>
          statement.setString(index + 1, encodeSubmissionLanguageColumn(language))
        }
        val stateStartIndex = languages.size + 1
        statement.setString(stateStartIndex, encodeSubmissionStatusColumn(runningState.status))
        setOptionalTimestamp(statement, stateStartIndex + 1, runningState.startedAt)
        setOptionalTimestamp(statement, stateStartIndex + 2, runningState.finishedAt)
        setOptionalVerdict(statement, stateStartIndex + 3, runningState.verdict)
        setOptionalJudgeMessage(statement, stateStartIndex + 4, runningState.judgeMessage)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ClaimedSubmission(
                id = SubmissionId(resultSet.getLong("public_id")),
                problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
                problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
                language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
                sourceCode = parseColumn("submissions.source_code", resultSet.getString("source_code"), SubmissionSourceCode.parse)
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val updateJudgeStateSQL: String =
    """
      |update submissions
      |set status = ?, verdict = ?, judge_message = ?, time_used_ms = ?, memory_used_kb = ?, score = ?, judge_result = ?::jsonb, started_at = ?, finished_at = ?
      |where public_id = ?
      |""".stripMargin

  def updateJudgeState(
    connection: Connection,
    submissionId: SubmissionId,
    judgeState: SubmissionJudgeState
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateJudgeStateSQL)
      try
        statement.setString(1, encodeSubmissionStatusColumn(judgeState.status))
        setOptionalVerdict(statement, 2, judgeState.verdict)
        setOptionalJudgeMessage(statement, 3, judgeState.judgeMessage)
        setOptionalLong(statement, 4, judgeState.timeUsedMs)
        setOptionalLong(statement, 5, judgeState.memoryUsedKb)
        setOptionalBigDecimal(statement, 6, judgeState.score)
        setOptionalJudgeResult(statement, 7, judgeState.judgeResult)
        setOptionalTimestamp(statement, 8, judgeState.startedAt)
        setOptionalTimestamp(statement, 9, judgeState.finishedAt)
        statement.setLong(10, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
