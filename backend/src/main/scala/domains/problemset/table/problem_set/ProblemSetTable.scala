package domains.problemset.table.problem_set



import cats.effect.IO
import cats.syntax.all.*
import database.utils.AccessGrantSql
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.problem.objects.{ProblemId}
import domains.problemset.objects.request.{CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.objects.{ProblemSet, ProblemSetId, ProblemSetSlug}
import domains.problemset.objects.response.ProblemSetSummary
import domains.problemset.table.problem_set_access_grant.ProblemSetAccessGrantTable
import shared.objects.access.{GrantRole, ResourceAccessPolicy}
import shared.objects.PageResponse
import domains.problemset.table.problem_set.ProblemSetTableSupport.*

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID
import database.utils.UserIdentitySql

object ProblemSetTable:

  enum AddProblemTableResult:
    case AlreadyLinked
    case Linked

  enum RemoveProblemTableResult:
    case NotLinked
    case Removed

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- ProblemSetTableSchema.initialize(connection)
      _ <- ProblemSetAccessGrantTable.initialize(connection)
    yield ()

  private val listSQL: String =
    s"""
      |select ps.id, ps.slug, ps.title, ps.description, ps.base_access, ${UserIdentitySql.selectOptionalColumns("ps.author_username", "author", "au")}, ps.created_at, ps.updated_at
      |from problem_sets ps
      |${UserIdentitySql.leftJoinUserProfiles("ps.author_username", "au")}
      |where
      |  ? = true
      |  or ps.base_access = 'public'
      |  or exists (
      |    select 1
      |    from problem_set_access_grants psag
      |    where psag.problem_set_id = ps.id
      |      and psag.grant_role = 'viewer'
      |      and psag.subject_kind = 'user'
      |      and psag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_access_grants psag
      |    join user_groups ug on ug.slug = psag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where psag.problem_set_id = ps.id
      |      and psag.grant_role = 'viewer'
      |      and psag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |order by ps.updated_at desc, ps.slug asc
      |limit ? offset ?
      |""".stripMargin

  private val countSQL: String =
    """
      |select count(*) as total_items
      |from problem_sets ps
      |where
      |  ? = true
      |  or ps.base_access = 'public'
      |  or exists (
      |    select 1
      |    from problem_set_access_grants psag
      |    where psag.problem_set_id = ps.id
      |      and psag.grant_role = 'viewer'
      |      and psag.subject_kind = 'user'
      |      and psag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_access_grants psag
      |    join user_groups ug on ug.slug = psag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where psag.problem_set_id = ps.id
      |      and psag.grant_role = 'viewer'
      |      and psag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |""".stripMargin

  def listVisibleTo(connection: Connection, actor: AuthenticatedUser, page: Int, pageSize: Int): IO[PageResponse[ProblemSetSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSQL)
        try
          bindAccessQuery(statement, actor, None, None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSQL)
        try
          bindAccessQuery(statement, actor, Some(pageSize), Some((page - 1) * pageSize))
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readProblemSetSummaryBase(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      itemsWithPolicies <- items.traverse { item =>
        for
          viewerGrants <- ProblemSetAccessGrantTable.listForProblemSet(connection, item.id, GrantRole.Viewer)
        yield item.copy(accessPolicy = item.accessPolicy.copy(viewerGrants = viewerGrants, managerGrants = Nil))
      }
    yield PageResponse(items = itemsWithPolicies, page = page, pageSize = pageSize, totalItems = totalItems)

  private val findBySlugSQL: String =
    s"""
      |select ps.id, ps.slug, ps.title, ps.description, ps.base_access, ${UserIdentitySql.selectOptionalColumns("ps.author_username", "author", "au")}, ps.created_at, ps.updated_at
      |from problem_sets ps
      |${UserIdentitySql.leftJoinUserProfiles("ps.author_username", "au")}
      |where ps.slug = ?
      |""".stripMargin

  private val listProblemsForSetSQL: String =
    """
      |select p.id, p.slug, p.title, psp.position
      |from problem_set_problems psp
      |join problems p on p.id = psp.problem_id
      |where psp.problem_set_id = ?
      |order by psp.position asc, p.slug asc
      |""".stripMargin

  def findBySlug(connection: Connection, slug: ProblemSetSlug): IO[Option[ProblemSet]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSQL)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemSetDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(problemSet) =>
        for
          problems <- listProblemsForSet(connection, problemSet.id, listProblemsForSetSQL)
          viewerGrants <- ProblemSetAccessGrantTable.listForProblemSet(connection, problemSet.id, GrantRole.Viewer)
        yield Some(problemSet.copy(problems = problems, accessPolicy = problemSet.accessPolicy.copy(viewerGrants = viewerGrants, managerGrants = Nil)))
      case None =>
        IO.pure(None)
    }

  private val insertSQL: String =
    s"""
      |insert into problem_sets (id, slug, title, description, base_access, author_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, description, base_access, ${UserIdentitySql.returningOptionalColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  def insert(connection: Connection, authorUsername: Username, request: CreateProblemSetRequest): IO[ProblemSet] =
    IO.blocking {
      val now = Instant.now()
      val problemSetId = ProblemSetId(UUID.randomUUID())
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, problemSetId.value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.description.value)
        statement.setString(5, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setString(6, authorUsername.value)
        statement.setTimestamp(7, Timestamp.from(now))
        statement.setTimestamp(8, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemSetDetailBase(resultSet).copy(problems = Nil)
          else missingInsertResult("problem set")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problemSet =>
      val sanitizedPolicy = sanitizePolicy(request.accessPolicy)
      ProblemSetAccessGrantTable
        .replaceForProblemSet(connection, problemSet.id, GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        .as(problemSet.copy(accessPolicy = sanitizedPolicy))
    }

  private val relationExistsSQL: String =
    """
      |select 1
      |from problem_set_problems
      |where problem_set_id = ? and problem_id = ?
      |""".stripMargin

  private val nextPositionSQL: String =
    """
      |select coalesce(max(position), 0) as current_max
      |from problem_set_problems
      |where problem_set_id = ?
      |""".stripMargin

  private val insertRelationSQL: String =
    """
      |insert into problem_set_problems (problem_set_id, problem_id, position)
      |values (?, ?, ?)
      |""".stripMargin

  def addProblem(connection: Connection, problemSetId: ProblemSetId, problemId: ProblemId): IO[AddProblemTableResult] =
    for
      alreadyLinked <- IO.blocking {
        val statement = connection.prepareStatement(relationExistsSQL)
        try
          statement.setObject(1, problemSetId.value)
          statement.setObject(2, problemId.value)
          val resultSet = statement.executeQuery()
          try resultSet.next()
          finally resultSet.close()
        finally statement.close()
      }
      result <- if alreadyLinked then
        IO.pure(AddProblemTableResult.AlreadyLinked)
      else
        IO.blocking {
          val nextPositionStatement = connection.prepareStatement(nextPositionSQL)
          try
            nextPositionStatement.setObject(1, problemSetId.value)
            val nextPositionResultSet = nextPositionStatement.executeQuery()
            val nextPosition =
              try
                if nextPositionResultSet.next() then nextPositionResultSet.getInt("current_max") + 1
                else 1
              finally nextPositionResultSet.close()

            val insertStatement = connection.prepareStatement(insertRelationSQL)
            try
              insertStatement.setObject(1, problemSetId.value)
              insertStatement.setObject(2, problemId.value)
              insertStatement.setInt(3, nextPosition)
              insertStatement.executeUpdate()
              AddProblemTableResult.Linked
            finally insertStatement.close()
          finally nextPositionStatement.close()
        }
    yield result

  private val updateSQL: String =
    """
      |update problem_sets
      |set title = ?, description = ?, base_access = ?, author_username = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def update(connection: Connection, problemSetId: ProblemSetId, request: UpdateProblemSetRequest): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSQL)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.description.value)
        statement.setString(3, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        request.authorUsername match
          case Some(username) => statement.setString(4, username.value)
          case None => statement.setNull(4, java.sql.Types.VARCHAR)
        statement.setTimestamp(5, Timestamp.from(now))
        statement.setObject(6, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    } *>
      ProblemSetAccessGrantTable.replaceForProblemSet(connection, problemSetId, GrantRole.Viewer, request.accessPolicy.viewerGrants)

  private val deleteSQL: String =
    """
      |delete from problem_sets
      |where id = ?
      |""".stripMargin

  def delete(connection: Connection, problemSetId: ProblemSetId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSQL)
      try
        statement.setObject(1, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val findRelationPositionSQL: String =
    """
      |select position
      |from problem_set_problems
      |where problem_set_id = ? and problem_id = ?
      |""".stripMargin

  private val deleteRelationSQL: String =
    """
      |delete from problem_set_problems
      |where problem_set_id = ? and problem_id = ?
      |""".stripMargin

  private val compactPositionsSQL: String =
    """
      |update problem_set_problems
      |set position = position - 1
      |where problem_set_id = ? and position > ?
      |""".stripMargin

  def removeProblem(connection: Connection, problemSetId: ProblemSetId, problemId: ProblemId): IO[RemoveProblemTableResult] =
    IO.blocking {
      val positionStatement = connection.prepareStatement(findRelationPositionSQL)
      try
        positionStatement.setObject(1, problemSetId.value)
        positionStatement.setObject(2, problemId.value)
        val resultSet = positionStatement.executeQuery()
        val maybePosition =
          try if resultSet.next() then Some(resultSet.getInt("position")) else None
          finally resultSet.close()

        maybePosition match
          case None =>
            RemoveProblemTableResult.NotLinked
          case Some(position) =>
            val deleteStatement = connection.prepareStatement(deleteRelationSQL)
            try
              deleteStatement.setObject(1, problemSetId.value)
              deleteStatement.setObject(2, problemId.value)
              deleteStatement.executeUpdate()
            finally deleteStatement.close()

            val compactStatement = connection.prepareStatement(compactPositionsSQL)
            try
              compactStatement.setObject(1, problemSetId.value)
              compactStatement.setInt(2, position)
              compactStatement.executeUpdate()
            finally compactStatement.close()

            RemoveProblemTableResult.Removed
      finally positionStatement.close()
    }

  private def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject)),
      managerGrants = Nil
    )
