package domains.problem.table.problem



import database.table.resource_access_grant.ResourceAccessGrantTable
import cats.effect.IO
import cats.syntax.all.*
import domains.user.model.Username
import domains.problem.application.input.{CreateProblemRequest, ProblemListRequest, UpdateProblemRequest}
import domains.problem.application.input.ProblemSearchQuery
import domains.problem.model.{OthersSubmissionAccess, ProblemData, ProblemDataFilename, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle}
import domains.problem.application.output.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import shared.access.{GrantRole, ResourceAccessPolicy, ResourceKind}
import database.utils.ResourceAccessTableSupport.{encodeBaseAccessColumn, missingInsertResult, policyFrom, sanitizePolicy, toLegacyVisibility}
import shared.model.PageResponse
import domains.problem.table.problem.ProblemTableSchema.*
import domains.problem.table.problem.ProblemTableSupport.*

import java.sql.{Connection, Timestamp}
import java.time.Instant
import shared.sql.UserIdentitySql

object ProblemTable:

  def initialize(connection: Connection): IO[Unit] =
    ProblemTableSchema.initialize(connection)

  private val visibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user'
      |      and rag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    join user_groups ug on ug.slug = rag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_problems psp
      |    join problem_sets ps on ps.id = psp.problem_set_id
      |    where psp.problem_id = p.id
      |      and (
      |        ? = true
      |        or ps.base_access = 'public'
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user'
      |            and rag.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          join user_groups ug on ug.slug = rag.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |      )
      |  )
      |)
      |""".stripMargin

  private val searchPredicate: String =
    """
      |(? = false or lower(p.slug) like lower(?) escape '\' or lower(p.title) like lower(?) escape '\')
      |""".stripMargin

  private val listSQL: String =
    s"""
      |select p.id, p.slug, p.title, p.data_name, p.ready, p.time_limit_ms, p.space_limit_mb, p.base_access, p.others_submission_access, ${UserIdentitySql.selectColumns("p.creator_username", "creator", "au")}, p.created_at, p.updated_at
      |from problems p
      |${UserIdentitySql.joinAuthUsers("p.creator_username", "au")}
      |where
      |  $visibilityPredicate
      |  and $searchPredicate
      |order by p.updated_at desc, p.slug asc
      |limit ? offset ?
      |""".stripMargin

  private val countSQL: String =
    s"""
      |select count(*) as total_items
      |from problems p
      |where
      |  $visibilityPredicate
      |  and $searchPredicate
      |""".stripMargin

  def listVisibleTo(connection: Connection, actor: domains.auth.model.AuthUser, request: ProblemListRequest): IO[PageResponse[ProblemSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSQL)
        try
          bindListQuery(statement, actor, request.query, pageSize = None, offset = None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSQL)
        try
          bindListQuery(
            statement,
            actor,
            request.query,
            pageSize = Some(request.pageRequest.pageSize),
            offset = Some((request.pageRequest.page - 1) * request.pageRequest.pageSize)
          )
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readProblemSummaryBase(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      itemsWithPolicies <- items.traverse { item =>
        for
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id), GrantRole.Manager)
        yield item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, viewerGrants, managerGrants))
      }
    yield PageResponse(
      items = itemsWithPolicies,
      page = request.pageRequest.page,
      pageSize = request.pageRequest.pageSize,
      totalItems = totalItems
    )

  private val findBySlugSQL: String =
    s"""
      |select p.id, p.slug, p.title, p.statement_text, p.data_name, p.ready, p.time_limit_ms, p.space_limit_mb, p.base_access, p.others_submission_access, ${UserIdentitySql.selectColumns("p.creator_username", "creator", "au")}, p.created_at, p.updated_at
      |from problems p
      |${UserIdentitySql.joinAuthUsers("p.creator_username", "au")}
      |where p.slug = ?
      |""".stripMargin

  def findBySlug(connection: Connection, slug: ProblemSlug): IO[Option[ProblemDetail]] =
    findBySlugUsing(connection, slug, findBySlugSQL)

  private val findBySlugForUpdateSQL: String =
    findBySlugSQL + "\nfor update of p"

  def findBySlugForUpdate(connection: Connection, slug: ProblemSlug): IO[Option[ProblemDetail]] =
    findBySlugUsing(connection, slug, findBySlugForUpdateSQL)

  private def findBySlugUsing(connection: Connection, slug: ProblemSlug, sql: String): IO[Option[ProblemDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(problem) =>
        for
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(problem.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(problem.id), GrantRole.Manager)
        yield Some(problem.copy(accessPolicy = policyFrom(problem.accessPolicy.baseAccess, viewerGrants, managerGrants)))
      case None =>
        IO.pure(None)
    }

  private val suggestionLimit: Int = 5

  private val suggestionOrderClause: String =
    """
      |case
      |  when lower(p.slug) = lower(?) then 0
      |  when lower(p.slug) like lower(?) escape '\' then 1
      |  when lower(p.title) like lower(?) escape '\' then 2
      |  when lower(p.slug) like lower(?) escape '\' then 3
      |  else 4
      |end,
      |p.slug asc
      |""".stripMargin

  private val listSuggestionsSQL: String =
    s"""
      |select p.slug, p.title
      |from problems p
      |where
      |  $visibilityPredicate
      |  and $searchPredicate
      |order by
      |  $suggestionOrderClause
      |limit $suggestionLimit
      |""".stripMargin

  def listSuggestions(connection: Connection, actor: domains.auth.model.AuthUser, query: ProblemSearchQuery): IO[List[ProblemSuggestion]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSuggestionsSQL)
      try
        bindSuggestionQuery(statement, actor, query)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readProblemSuggestion(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val hasVisibleContainingProblemSetSQL: String =
    """
      |select 1
      |from problem_set_problems psp
      |join problem_sets ps on ps.id = psp.problem_set_id
      |where psp.problem_id = ?
      |  and (
      |    ? = true
      |    or ps.base_access = 'public'
      |    or exists (
      |      select 1
      |      from resource_access_grants rag
      |      where rag.resource_kind = 'problem_set'
      |        and rag.resource_id = ps.id
      |        and rag.grant_role = 'viewer'
      |        and rag.subject_kind = 'user'
      |        and rag.subject_key = ?
      |    )
      |    or exists (
      |      select 1
      |      from resource_access_grants rag
      |      join user_groups ug on ug.slug = rag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where rag.resource_kind = 'problem_set'
      |        and rag.resource_id = ps.id
      |        and rag.grant_role = 'viewer'
      |        and rag.subject_kind = 'user_group'
      |        and ugm.username = ?
      |    )
      |  )
      |limit 1
      |""".stripMargin

  def hasVisibleContainingProblemSet(
    connection: Connection,
    actor: domains.auth.model.AuthUser,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasVisibleContainingProblemSetSQL)
      try
        bindContainingProblemSetVisibilityQuery(statement, actor, problemId)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val insertSQL: String =
    s"""
      |insert into problems (id, slug, title, statement_text, data_name, data_bytes, time_limit_ms, space_limit_mb, visibility, base_access, others_submission_access, creator_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, data_name, ready, time_limit_ms, space_limit_mb, base_access, others_submission_access, ${UserIdentitySql.returningColumns("creator_username", "creator")}, created_at, updated_at
      |""".stripMargin

  def insert(
    connection: Connection,
    problemId: ProblemId,
    createdAt: Instant,
    creatorUsername: Username,
    request: CreateProblemRequest
  ): IO[ProblemDetail] =
    IO.blocking {
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, problemId.value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.statement.value)
        statement.setNull(5, java.sql.Types.VARCHAR)
        statement.setNull(6, java.sql.Types.BINARY)
        statement.setInt(7, request.timeLimitMs.value)
        statement.setInt(8, request.spaceLimitMb.value)
        statement.setString(9, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(10, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setString(11, encodeOthersSubmissionAccessColumn(request.othersSubmissionAccess))
        statement.setString(12, creatorUsername.value)
        statement.setTimestamp(13, Timestamp.from(createdAt))
        statement.setTimestamp(14, Timestamp.from(createdAt))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemDetailBase(resultSet)
          else missingInsertResult("problem")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problem =>
      val sanitizedPolicy = sanitizePolicy(request.accessPolicy)
      ResourceAccessGrantTable
        .replaceForResource(connection, ResourceKind.Problem, toResourceId(problem.id), GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        .flatMap(_ =>
          ResourceAccessGrantTable.replaceForResource(
            connection,
            ResourceKind.Problem,
            toResourceId(problem.id),
            GrantRole.Manager,
            sanitizedPolicy.managerGrants
          )
        )
        .as(problem.copy(accessPolicy = sanitizedPolicy))
    }

  private val updateSQL: String =
    """
      |update problems
      |set title = ?, statement_text = ?, time_limit_ms = ?, space_limit_mb = ?, visibility = ?, base_access = ?, others_submission_access = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def update(connection: Connection, problemId: ProblemId, updatedAt: Instant, request: UpdateProblemRequest): IO[Unit] =
    for
      _ <- IO.blocking {
      val statement = connection.prepareStatement(updateSQL)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.statement.value)
        statement.setInt(3, request.timeLimitMs.value)
        statement.setInt(4, request.spaceLimitMb.value)
        statement.setString(5, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(6, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setString(7, encodeOthersSubmissionAccessColumn(request.othersSubmissionAccess))
        statement.setTimestamp(8, Timestamp.from(updatedAt))
        statement.setObject(9, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
      }
      _ <- ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), GrantRole.Viewer, request.accessPolicy.viewerGrants)
      _ <- ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), GrantRole.Manager, request.accessPolicy.managerGrants)
    yield ()

  def updateData(connection: Connection, problemId: ProblemId, updatedAt: Instant, filename: ProblemDataFilename): IO[Unit] =
    updateData(connection, problemId, updatedAt, Some(filename))

  private val updateDataSQL: String =
    """
      |update problems
      |set data_name = ?, ready = false, updated_at = ?
      |where id = ?
      |""".stripMargin

  def updateData(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename]
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataSQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setTimestamp(2, Timestamp.from(updatedAt))
        statement.setObject(3, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val updateDataReadySQL: String =
    """
      |update problems
      |set data_name = ?, ready = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def updateDataReady(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename],
    ready: Boolean
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataReadySQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setBoolean(2, ready)
        statement.setTimestamp(3, Timestamp.from(updatedAt))
        statement.setObject(4, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteSQL: String =
    """
      |delete from problems
      |where id = ?
      |""".stripMargin

  def delete(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSQL)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
