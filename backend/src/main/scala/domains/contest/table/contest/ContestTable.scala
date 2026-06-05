package domains.contest.table.contest

import cats.effect.IO
import cats.syntax.all.*
import database.utils.{AccessGrantSql, UserIdentitySql}
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.*
import domains.contest.objects.request.{CreateContestRequest, UpdateContestRequest}
import domains.contest.objects.response.{ContestRegistrant, ContestRegistrationStatus, ContestSummary}
import domains.contest.table.contest.ContestTableSupport.*
import domains.contest.table.contest_access_grant.ContestAccessGrantTable
import domains.problem.objects.ProblemId
import domains.user.objects.Username
import shared.objects.PageResponse
import shared.objects.access.{GrantRole, ResourceAccessPolicy}

import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.Instant
import java.util.UUID

object ContestTable:

  enum AddProblemTableResult:
    case AlreadyLinked
    case Linked

  enum RegisterTableResult:
    case AlreadyRegistered
    case Registered

  enum UnregisterTableResult:
    case NotRegistered
    case Unregistered

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- ContestTableSchema.initialize(connection)
      _ <- ContestAccessGrantTable.initialize(connection)
    yield ()

  private val listSQL: String =
    s"""
      |select c.id, c.slug, c.title, c.description, c.start_at, c.end_at, c.base_access, ${UserIdentitySql.selectOptionalColumns("c.author_username", "author", "au")}, c.created_at, c.updated_at
      |from contests c
      |${UserIdentitySql.leftJoinUserProfiles("c.author_username", "au")}
      |where
      |  ? = true
      |  or c.base_access = 'public'
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user'
      |      and cag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    join user_groups ug on ug.slug = cag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |order by c.start_at desc, c.created_at desc, c.slug asc
      |limit ? offset ?
      |""".stripMargin

  private val countSQL: String =
    """
      |select count(*) as total_items
      |from contests c
      |where
      |  ? = true
      |  or c.base_access = 'public'
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user'
      |      and cag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    join user_groups ug on ug.slug = cag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |""".stripMargin

  def listVisibleTo(connection: Connection, actor: AuthenticatedUser, page: Int, pageSize: Int): IO[PageResponse[ContestSummary]] =
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
              .map(_ => readContestSummaryBase(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      itemsWithPolicies <- items.traverse { item =>
        for
          viewerGrants <- ContestAccessGrantTable.listForContest(connection, item.id, GrantRole.Viewer)
          managerGrants <- ContestAccessGrantTable.listForContest(connection, item.id, GrantRole.Manager)
          registration <- findRegistration(connection, item.id, actor.username)
        yield item.copy(
          accessPolicy = item.accessPolicy.copy(viewerGrants = viewerGrants, managerGrants = managerGrants),
          registrationStatus = registration.fold(ContestRegistrationStatus.notRegistered)(ContestRegistrationStatus.registeredAt)
        )
      }
    yield PageResponse(items = itemsWithPolicies, page = page, pageSize = pageSize, totalItems = totalItems)

  private def bindAccessQuery(statement: PreparedStatement, actor: AuthenticatedUser, limit: Option[Int], offset: Option[Int]): Unit =
    val isGlobalContestManager = actor.siteManager || actor.contestManager
    statement.setBoolean(1, isGlobalContestManager)
    statement.setString(2, actor.username.value)
    statement.setString(3, actor.username.value)
    (limit, offset) match
      case (Some(limitValue), Some(offsetValue)) =>
        statement.setInt(4, limitValue)
        statement.setInt(5, offsetValue)
      case _ =>
        ()

  private val findBySlugSQL: String =
    s"""
      |select c.id, c.slug, c.title, c.description, c.start_at, c.end_at, c.base_access, ${UserIdentitySql.selectOptionalColumns("c.author_username", "author", "au")}, c.created_at, c.updated_at
      |from contests c
      |${UserIdentitySql.leftJoinUserProfiles("c.author_username", "au")}
      |where c.slug = ?
      |""".stripMargin

  def findBySlug(connection: Connection, slug: ContestSlug): IO[Option[Contest]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSQL)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readContestBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(contest) =>
        for
          problems <- listProblemsForContest(connection, contest.id)
          viewerGrants <- ContestAccessGrantTable.listForContest(connection, contest.id, GrantRole.Viewer)
          managerGrants <- ContestAccessGrantTable.listForContest(connection, contest.id, GrantRole.Manager)
        yield Some(contest.copy(problems = problems, accessPolicy = contest.accessPolicy.copy(viewerGrants = viewerGrants, managerGrants = managerGrants)))
      case None =>
        IO.pure(None)
    }

  private val insertSQL: String =
    s"""
      |insert into contests (id, slug, title, description, start_at, end_at, base_access, author_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, description, start_at, end_at, base_access, ${UserIdentitySql.returningOptionalColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  def insert(connection: Connection, authorUsername: Username, request: CreateContestRequest): IO[Contest] =
    IO.blocking {
      val now = Instant.now()
      val contestId = ContestId(UUID.randomUUID())
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, contestId.value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.description.value)
        statement.setTimestamp(5, Timestamp.from(request.startAt))
        statement.setTimestamp(6, Timestamp.from(request.endAt))
        statement.setString(7, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setString(8, authorUsername.value)
        statement.setTimestamp(9, Timestamp.from(now))
        statement.setTimestamp(10, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readContestBase(resultSet)
          else missingInsertResult("contest")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { contest =>
      val sanitizedPolicy = sanitizePolicy(request.accessPolicy)
      for
        _ <- ContestAccessGrantTable.replaceForContest(connection, contest.id, GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        _ <- ContestAccessGrantTable.replaceForContest(connection, contest.id, GrantRole.Manager, sanitizedPolicy.managerGrants)
      yield contest.copy(accessPolicy = sanitizedPolicy)
    }

  private val updateSQL: String =
    s"""
      |update contests
      |set title = ?,
      |    description = ?,
      |    start_at = ?,
      |    end_at = ?,
      |    base_access = ?,
      |    updated_at = ?
      |where id = ?
      |returning id, slug, title, description, start_at, end_at, base_access, ${UserIdentitySql.returningOptionalColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  def update(connection: Connection, contest: Contest, request: UpdateContestRequest): IO[Contest] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSQL)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.description.value)
        statement.setTimestamp(3, Timestamp.from(request.startAt))
        statement.setTimestamp(4, Timestamp.from(request.endAt))
        statement.setString(5, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setTimestamp(6, Timestamp.from(now))
        statement.setObject(7, contest.id.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readContestBase(resultSet)
          else missingInsertResult("contest")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { updatedContest =>
      val sanitizedPolicy = sanitizePolicy(request.accessPolicy)
      for
        _ <- ContestAccessGrantTable.replaceForContest(connection, updatedContest.id, GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        _ <- ContestAccessGrantTable.replaceForContest(connection, updatedContest.id, GrantRole.Manager, sanitizedPolicy.managerGrants)
        problems <- listProblemsForContest(connection, updatedContest.id)
      yield updatedContest.copy(problems = problems, accessPolicy = sanitizedPolicy)
    }

  private val listProblemsForContestSQL: String =
    """
      |select p.id, p.slug, p.title, cp.position, cp.alias
      |from contest_problems cp
      |join problems p on p.id = cp.problem_id
      |where cp.contest_id = ?
      |order by cp.position asc, p.slug asc
      |""".stripMargin

  private def listProblemsForContest(connection: Connection, contestId: ContestId): IO[List[ContestProblemSummary]] =
    ContestTableSupport.listProblemsForContest(connection, contestId, listProblemsForContestSQL)

  private val relationExistsSQL: String =
    """
      |select 1
      |from contest_problems
      |where contest_id = ? and problem_id = ?
      |""".stripMargin

  private val nextPositionSQL: String =
    """
      |select coalesce(max(position), 0) as current_max
      |from contest_problems
      |where contest_id = ?
      |""".stripMargin

  private val insertRelationSQL: String =
    """
      |insert into contest_problems (contest_id, problem_id, position, alias)
      |values (?, ?, ?, ?)
      |""".stripMargin

  def addProblem(connection: Connection, contestId: ContestId, problemId: ProblemId): IO[AddProblemTableResult] =
    for
      alreadyLinked <- IO.blocking {
        val statement = connection.prepareStatement(relationExistsSQL)
        try
          statement.setObject(1, contestId.value)
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
            nextPositionStatement.setObject(1, contestId.value)
            val nextPositionResultSet = nextPositionStatement.executeQuery()
            val nextPosition =
              try if nextPositionResultSet.next() then nextPositionResultSet.getInt("current_max") + 1 else 1
              finally nextPositionResultSet.close()

            val insertStatement = connection.prepareStatement(insertRelationSQL)
            try
              insertStatement.setObject(1, contestId.value)
              insertStatement.setObject(2, problemId.value)
              insertStatement.setInt(3, nextPosition)
              insertStatement.setString(4, ContestProblemAlias.fromPosition(nextPosition).value)
              insertStatement.executeUpdate()
              AddProblemTableResult.Linked
            finally insertStatement.close()
          finally nextPositionStatement.close()
        }
    yield result

  private val deleteRelationSQL: String =
    """
      |delete from contest_problems
      |where contest_id = ? and problem_id = ?
      |""".stripMargin

  def removeProblem(connection: Connection, contestId: ContestId, problemId: ProblemId): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteRelationSQL)
      try
        statement.setObject(1, contestId.value)
        statement.setObject(2, problemId.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val findRegistrationSQL: String =
    """
      |select registered_at
      |from contest_registrations
      |where contest_id = ? and username = ?
      |""".stripMargin

  def findRegistration(connection: Connection, contestId: ContestId, username: Username): IO[Option[Instant]] =
    IO.blocking {
      val statement = connection.prepareStatement(findRegistrationSQL)
      try
        statement.setObject(1, contestId.value)
        statement.setString(2, username.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(resultSet.getTimestamp("registered_at").toInstant) else None
        finally resultSet.close()
      finally statement.close()
    }

  private val insertRegistrationSQL: String =
    """
      |insert into contest_registrations (contest_id, username, registered_at)
      |values (?, ?, ?)
      |""".stripMargin

  def register(connection: Connection, contestId: ContestId, username: Username): IO[RegisterTableResult] =
    for
      existing <- findRegistration(connection, contestId, username)
      result <- existing match
        case Some(_) =>
          IO.pure(RegisterTableResult.AlreadyRegistered)
        case None =>
          IO.blocking {
            val statement = connection.prepareStatement(insertRegistrationSQL)
            try
              statement.setObject(1, contestId.value)
              statement.setString(2, username.value)
              statement.setTimestamp(3, Timestamp.from(Instant.now()))
              statement.executeUpdate()
              RegisterTableResult.Registered
            finally statement.close()
          }
    yield result

  private val deleteRegistrationSQL: String =
    """
      |delete from contest_registrations
      |where contest_id = ? and username = ?
      |""".stripMargin

  def unregister(connection: Connection, contestId: ContestId, username: Username): IO[UnregisterTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteRegistrationSQL)
      try
        statement.setObject(1, contestId.value)
        statement.setString(2, username.value)
        if statement.executeUpdate() > 0 then UnregisterTableResult.Unregistered
        else UnregisterTableResult.NotRegistered
      finally statement.close()
    }

  private val listRegistrantsSQL: String =
    s"""
      |select cr.username as user_username, up.display_name as user_display_name, cr.registered_at
      |from contest_registrations cr
      |${UserIdentitySql.joinUserProfiles("cr.username", "up")}
      |where cr.contest_id = ?
      |order by cr.registered_at asc, cr.username asc
      |limit ? offset ?
      |""".stripMargin

  private val countRegistrantsSQL: String =
    """
      |select count(*) as total_items
      |from contest_registrations cr
      |where cr.contest_id = ?
      |""".stripMargin

  def listRegistrants(connection: Connection, contestId: ContestId, page: Int, pageSize: Int): IO[PageResponse[ContestRegistrant]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countRegistrantsSQL)
        try
          statement.setObject(1, contestId.value)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listRegistrantsSQL)
        try
          statement.setObject(1, contestId.value)
          statement.setInt(2, pageSize)
          statement.setInt(3, (page - 1) * pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readContestRegistrant(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(items = items, page = page, pageSize = pageSize, totalItems = totalItems)

  private def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject)),
      managerGrants = policy.managerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject))
    )
