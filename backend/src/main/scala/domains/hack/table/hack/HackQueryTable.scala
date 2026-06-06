package domains.hack.table.hack

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.HackId
import domains.hack.objects.response.{HackDetail, HackListResponse, HackSummary}
import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.SubmissionId
import domains.hack.table.hack.HackTableSupport.*
import shared.objects.PageResponse

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.util.UUID

object HackQueryTable:

  private val visibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or ? = true
      |  or h.author_username = ?
      |  or target.submitter_username = ?
      |  or (
      |    p.other_user_submission_access = 'detail'
      |    and p.base_access = 'public'
      |  )
      |)
      |""".stripMargin

  private val selectColumns: String =
    """
      |h.public_id,
      |h.problem_id,
      |p.slug as problem_slug,
      |p.title as problem_title,
      |target.public_id as target_submission_public_id,
      |target.submitter_username as target_username,
      |target_au.display_name as target_display_name,
      |h.author_username as author_username,
      |author_au.display_name as author_display_name,
      |h.subtask_index,
      |h.subtask_label,
      |h.status,
      |h.input_text,
      |h.strategy_provider_source,
      |h.answer_text,
      |h.old_score,
      |h.new_score,
      |h.validator_message,
      |h.standard_message,
      |h.target_message,
      |h.created_at,
      |h.started_at,
      |h.finished_at
      |""".stripMargin

  private val fromJoins: String =
    """
      |from hack_attempts h
      |join problems p on p.id = h.problem_id
      |join submissions target on target.public_id = h.target_submission_public_id
      |join user_profiles target_au on target_au.username = target.submitter_username
      |join user_profiles author_au on author_au.username = h.author_username
      |""".stripMargin

  private val findVisibleByIdSQL: String =
    s"""
      |select $selectColumns
      |$fromJoins
      |where h.public_id = ?
      |  and $visibilityPredicate
      |""".stripMargin

  def findVisibleById(connection: Connection, actor: AuthenticatedUser, hackId: HackId): IO[Option[HackDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(findVisibleByIdSQL)
      try
        statement.setLong(1, hackId.value)
        bindVisibility(statement, 2, actor)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readHackDetail(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private val countVisibleSQL: String =
    s"""
      |select count(*) as total_items
      |$fromJoins
      |where $visibilityPredicate
      |""".stripMargin

  private val listVisibleSQL: String =
    s"""
      |select $selectColumns
      |$fromJoins
      |where $visibilityPredicate
      |order by h.created_at desc, h.public_id desc
      |limit ? offset ?
      |""".stripMargin

  def listVisible(connection: Connection, actor: AuthenticatedUser, page: Int, pageSize: Int): IO[HackListResponse] =
    val normalizedPage = math.max(1, page)
    val normalizedPageSize = math.max(1, math.min(100, pageSize))
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countVisibleSQL)
        try
          bindVisibility(statement, 1, actor)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listVisibleSQL)
        try
          val nextIndex = bindVisibility(statement, 1, actor)
          statement.setInt(nextIndex, normalizedPageSize)
          statement.setInt(nextIndex + 1, (normalizedPage - 1) * normalizedPageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readHackSummary(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(items = items, page = normalizedPage, pageSize = normalizedPageSize, totalItems = totalItems)

  private def bindVisibility(statement: PreparedStatement, startIndex: Int, actor: AuthenticatedUser): Int =
    statement.setBoolean(startIndex, actor.siteManager)
    statement.setBoolean(startIndex + 1, actor.problemManager)
    statement.setString(startIndex + 2, actor.username.value)
    statement.setString(startIndex + 3, actor.username.value)
    startIndex + 4

  private def readHackSummary(resultSet: ResultSet): HackSummary =
    HackSummary(
      id = HackId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[UUID])),
      problemSlug = ProblemSlug(resultSet.getString("problem_slug")),
      problemTitle = ProblemTitle(resultSet.getString("problem_title")),
      targetSubmissionId = SubmissionId(resultSet.getLong("target_submission_public_id")),
      targetSubmitter = readUserIdentity(resultSet, "target"),
      author = readUserIdentity(resultSet, "author"),
      subtaskIndex = resultSet.getInt("subtask_index"),
      subtaskLabel = Option(resultSet.getString("subtask_label")),
      status = decodeHackStatusColumn(resultSet.getString("status")),
      oldScore = BigDecimal(resultSet.getBigDecimal("old_score")),
      newScore = Option(resultSet.getBigDecimal("new_score")).map(BigDecimal(_)),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
    )

  private def readHackDetail(resultSet: ResultSet): HackDetail =
    HackDetail(
      id = HackId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[UUID])),
      problemSlug = ProblemSlug(resultSet.getString("problem_slug")),
      problemTitle = ProblemTitle(resultSet.getString("problem_title")),
      targetSubmissionId = SubmissionId(resultSet.getLong("target_submission_public_id")),
      targetSubmitter = readUserIdentity(resultSet, "target"),
      author = readUserIdentity(resultSet, "author"),
      subtaskIndex = resultSet.getInt("subtask_index"),
      subtaskLabel = Option(resultSet.getString("subtask_label")),
      status = decodeHackStatusColumn(resultSet.getString("status")),
      input = resultSet.getString("input_text"),
      strategyProviderSource = Option(resultSet.getString("strategy_provider_source")),
      answer = Option(resultSet.getString("answer_text")),
      oldScore = BigDecimal(resultSet.getBigDecimal("old_score")),
      newScore = Option(resultSet.getBigDecimal("new_score")).map(BigDecimal(_)),
      validatorMessage = Option(resultSet.getString("validator_message")),
      standardMessage = Option(resultSet.getString("standard_message")),
      targetMessage = Option(resultSet.getString("target_message")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
      finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
    )
