package domains.submission.table

import cats.effect.IO
import domains.auth.model.{AuthUser, DisplayName, UserIdentity, Username}
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.application.SubmissionPolicy
import domains.submission.model.{SubmissionDetail, SubmissionId, SubmissionJudgeState, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionSummary, SubmissionVerdict}
import domains.submission.table.SubmissionTableSchema.*
import domains.submission.table.SubmissionTableSql.*
import domains.submission.table.SubmissionTableSupport.*

import java.nio.charset.StandardCharsets
import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  timeLimitMs: Int,
  spaceLimitMb: Int
)

object SubmissionTable:

  def initialize(connection: Connection): IO[Unit] =
    SubmissionTableSchema.initialize(connection)

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
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setObject(2, problemId.value)
        statement.setString(3, submitterUsername.value)
        statement.setString(4, SubmissionLanguage.toDatabase(language))
        statement.setString(5, SubmissionStatus.toDatabase(SubmissionStatus.Queued))
        statement.setNull(6, java.sql.Types.VARCHAR)
        statement.setNull(7, java.sql.Types.LONGVARCHAR)
        statement.setNull(8, java.sql.Types.BIGINT)
        statement.setNull(9, java.sql.Types.BIGINT)
        statement.setString(10, sourceCode.value)
        statement.setTimestamp(11, Timestamp.from(now))
        statement.setNull(12, java.sql.Types.TIMESTAMP)
        statement.setNull(13, java.sql.Types.TIMESTAMP)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            SubmissionDetail(
              id = SubmissionId(resultSet.getLong("public_id")),
              problemId = problemId,
              problemSlug = problemSlug,
              problemTitle = problemTitle,
              canManage = false,
              submitter = UserIdentity(
                submitterUsername,
                DisplayName(resultSet.getString("submitter_display_name"))
              ),
              language = language,
              status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
              verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
              judgeMessage = Option(resultSet.getString("judge_message")),
              timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
              memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
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

  def listVisibleTo(connection: Connection, actor: AuthUser, submitterUsername: Option[Username]): IO[List[SubmissionSummary]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSql)
      try
        statement.setBoolean(1, SubmissionPolicy.hasGlobalViewOverride(actor))
        statement.setString(2, actor.username.value)
        statement.setString(3, actor.username.value)
        statement.setString(4, actor.username.value)
        statement.setBoolean(5, SubmissionPolicy.hasGlobalViewOverride(actor))
        statement.setString(6, actor.username.value)
        statement.setString(7, actor.username.value)
        statement.setBoolean(8, submitterUsername.nonEmpty)
        statement.setString(9, submitterUsername.map(_.value).getOrElse(""))
        statement.setBoolean(10, SubmissionPolicy.hasGlobalViewOverride(actor))
        statement.setString(11, actor.username.value)
        statement.setString(12, actor.username.value)
        statement.setString(13, actor.username.value)
        statement.setBoolean(14, SubmissionPolicy.hasGlobalViewOverride(actor))
        statement.setString(15, actor.username.value)
        statement.setString(16, actor.username.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readSubmissionSummary(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def findById(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSql)
      try
        statement.setLong(1, submissionId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readSubmissionDetail(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  def claimNextCpp17(connection: Connection, runningState: SubmissionJudgeState): IO[Option[ClaimedSubmission]] =
    IO.blocking {
      val statement = connection.prepareStatement(claimNextCpp17Sql)
      try
        statement.setString(1, SubmissionStatus.toDatabase(runningState.status))
        setOptionalTimestamp(statement, 2, runningState.startedAt)
        setOptionalTimestamp(statement, 3, runningState.finishedAt)
        setOptionalVerdict(statement, 4, runningState.verdict)
        setOptionalJudgeMessage(statement, 5, runningState.judgeMessage)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ClaimedSubmission(
                id = SubmissionId(resultSet.getLong("public_id")),
                problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
                problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
                language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
                sourceCode = parseColumn("submissions.source_code", resultSet.getString("source_code"), SubmissionSourceCode.parse),
                timeLimitMs = resultSet.getInt("time_limit_ms"),
                spaceLimitMb = resultSet.getInt("space_limit_mb")
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def updateJudgeState(
    connection: Connection,
    submissionId: SubmissionId,
    judgeState: SubmissionJudgeState
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateJudgeStateSql)
      try
        statement.setString(1, SubmissionStatus.toDatabase(judgeState.status))
        setOptionalVerdict(statement, 2, judgeState.verdict)
        setOptionalJudgeMessage(statement, 3, judgeState.judgeMessage)
        setOptionalLong(statement, 4, judgeState.timeUsedMs)
        setOptionalLong(statement, 5, judgeState.memoryUsedKb)
        setOptionalTimestamp(statement, 6, judgeState.startedAt)
        setOptionalTimestamp(statement, 7, judgeState.finishedAt)
        statement.setLong(8, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def deleteById(connection: Connection, submissionId: SubmissionId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByIdSql)
      try
        statement.setLong(1, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
