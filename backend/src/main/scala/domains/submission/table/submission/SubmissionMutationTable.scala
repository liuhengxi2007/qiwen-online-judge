package domains.submission.table.submission

import cats.effect.IO
import database.utils.UserIdentitySql
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.model.{SubmissionId, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus}
import domains.submission.model.response.SubmissionDetail
import domains.submission.table.submission.SubmissionTableSupport.*
import domains.user.model.Username

import java.nio.charset.StandardCharsets
import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object SubmissionMutationTable:

  private val insertSQL: String =
    s"""
      |insert into submissions (id, public_id, problem_id, submitter_username, language, status, verdict, judge_message, time_used_ms, memory_used_kb, score, judge_result, source_code, submitted_at, started_at, finished_at)
      |values (?, nextval('submission_public_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
      |returning public_id, status, verdict, judge_message, time_used_ms, memory_used_kb, score, judge_result::text as judge_result, ${UserIdentitySql.returningColumns("submitter_username", "submitter")}, submitted_at, started_at, finished_at
      |""".stripMargin

  def insert(
    connection: Connection,
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    problemTitle: ProblemTitle,
    submitterUsername: Username,
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  ): IO[SubmissionDetail] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setObject(2, problemId.value)
        statement.setString(3, submitterUsername.value)
        statement.setString(4, encodeSubmissionLanguageColumn(language))
        statement.setString(5, encodeSubmissionStatusColumn(SubmissionStatus.Queued))
        statement.setNull(6, java.sql.Types.VARCHAR)
        statement.setNull(7, java.sql.Types.LONGVARCHAR)
        statement.setNull(8, java.sql.Types.BIGINT)
        statement.setNull(9, java.sql.Types.BIGINT)
        statement.setNull(10, java.sql.Types.NUMERIC)
        statement.setNull(11, java.sql.Types.VARCHAR)
        statement.setString(12, sourceCode.value)
        statement.setTimestamp(13, Timestamp.from(now))
        statement.setNull(14, java.sql.Types.TIMESTAMP)
        statement.setNull(15, java.sql.Types.TIMESTAMP)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            SubmissionDetail(
              id = SubmissionId(resultSet.getLong("public_id")),
              problemId = problemId,
              problemSlug = problemSlug,
              problemTitle = problemTitle,
              canManage = false,
              submitter = readUserIdentity(resultSet, "submitter"),
              language = language,
              status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
              verdict = Option(resultSet.getString("verdict")).flatMap(decodeSubmissionVerdictColumn),
              judgeMessage = Option(resultSet.getString("judge_message")),
              timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
              memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
              score = readOptionalBigDecimal(resultSet, "score"),
              judgeResult = readOptionalJudgeResult(resultSet, "judge_result"),
              codeLength = sourceCode.value.getBytes(StandardCharsets.UTF_8).length,
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
