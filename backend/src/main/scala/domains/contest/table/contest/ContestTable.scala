package domains.contest.table.contest

import cats.effect.IO
import database.utils.{AccessGrantSql, UserIdentitySql}
import domains.contest.objects.*
import domains.contest.objects.request.CreateContestRequest
import domains.contest.table.contest.ContestTableSupport.*
import domains.contest.table.contest_access_grant.ContestAccessGrantTable
import domains.problem.objects.ProblemId
import domains.user.objects.Username
import shared.objects.access.{GrantRole, ResourceAccessPolicy}

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

object ContestTable:

  enum AddProblemTableResult:
    case AlreadyLinked
    case Linked

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- ContestTableSchema.initialize(connection)
      _ <- ContestAccessGrantTable.initialize(connection)
    yield ()

  private val findBySlugSQL: String =
    s"""
      |select c.id, c.slug, c.title, c.description, c.start_at, c.end_at, c.base_access, ${UserIdentitySql.selectOptionalColumns("c.author_username", "author", "au")}, c.created_at, c.updated_at
      |from contests c
      |${UserIdentitySql.leftJoinUserProfiles("c.author_username", "au")}
      |where c.slug = ?
      |""".stripMargin

  private val listProblemsForContestSQL: String =
    """
      |select p.id, p.slug, p.title, cp.position, cp.alias
      |from contest_problems cp
      |join problems p on p.id = cp.problem_id
      |where cp.contest_id = ?
      |order by cp.position asc, p.slug asc
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
          problems <- listProblemsForContest(connection, contest.id, listProblemsForContestSQL)
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

  private def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject)),
      managerGrants = policy.managerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject))
    )
