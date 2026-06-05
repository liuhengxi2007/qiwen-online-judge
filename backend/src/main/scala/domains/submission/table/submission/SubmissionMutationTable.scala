package domains.submission.table.submission

import cats.effect.IO
import database.utils.UserIdentitySql
import domains.contest.objects.ContestId
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.{SubmissionId, SubmissionSourceCode, SubmissionStatus}
import domains.submission.objects.internal.SubmissionProgramManifest
import domains.submission.objects.response.{SubmissionDetail, SubmissionSource}
import domains.submission.table.submission.SubmissionTableSupport.*
import domains.user.objects.Username
import io.circe.syntax.*

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object SubmissionMutationTable:

  private val insertSQL: String =
    s"""
      |insert into submissions (id, public_id, problem_id, contest_id, submitter_username, program_manifest, status, judge_result, submitted_at, started_at, finished_at)
      |values (?, nextval('submission_public_id_seq'), ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?, ?)
      |returning public_id, language, status, verdict, time_used_ms, memory_used_kb, score, judge_result::text as judge_result, code_length, null::varchar as source_contest_slug, null::varchar as source_contest_title, ${UserIdentitySql.returningColumns("submitter_username", "submitter")}, submitted_at, started_at, finished_at
      |""".stripMargin

  def insert(
    connection: Connection,
    submissionUuid: UUID,
    problemId: ProblemId,
    contestId: Option[ContestId],
    problemSlug: ProblemSlug,
    problemTitle: ProblemTitle,
    source: SubmissionSource,
    submitterUsername: Username,
    programManifest: SubmissionProgramManifest,
    sourceCode: SubmissionSourceCode
  ): IO[SubmissionDetail] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, submissionUuid)
        statement.setObject(2, problemId.value)
        contestId match
          case Some(value) => statement.setObject(3, value.value)
          case None => statement.setNull(3, java.sql.Types.OTHER)
        statement.setString(4, submitterUsername.value)
        statement.setString(5, programManifest.asJson.noSpaces)
        statement.setString(6, encodeSubmissionStatusColumn(SubmissionStatus.Queued))
        statement.setNull(7, java.sql.Types.VARCHAR)
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setNull(9, java.sql.Types.TIMESTAMP)
        statement.setNull(10, java.sql.Types.TIMESTAMP)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            SubmissionDetail(
              id = SubmissionId(resultSet.getLong("public_id")),
              problemId = problemId,
              problemSlug = problemSlug,
              problemTitle = problemTitle,
              source = source,
              canManage = false,
              submitter = readUserIdentity(resultSet, "submitter"),
              language = parseColumn("submissions.language", resultSet.getString("language"), domains.submission.objects.SubmissionLanguage.parse),
              status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
              verdict = Option(resultSet.getString("verdict")).flatMap(decodeSubmissionVerdictColumn),
              timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
              memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
              score = readOptionalBigDecimal(resultSet, "score"),
              judgeResult = readOptionalJudgeResult(resultSet, "judge_result"),
              codeLength = resultSet.getInt("code_length"),
              sourceCode = sourceCode,
              submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
              startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
              finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
            )
          else missingInsertResult("submission")
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteByIdSQL: String =
    """
      |delete from submissions
      |where public_id = ?
      |""".stripMargin

  def deleteById(connection: Connection, submissionId: SubmissionId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByIdSQL)
      try
        statement.setLong(1, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
