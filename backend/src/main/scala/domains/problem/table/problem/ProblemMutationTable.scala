package domains.problem.table.problem

import cats.effect.IO
import database.utils.AccessGrantSql
import database.utils.UserIdentitySql
import domains.problem.objects.request.{CreateProblemRequest, UpdateProblemRequest}
import domains.problem.objects.{ProblemId}
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemTableSupport.*
import domains.problem.table.problem_access_grant.ProblemAccessGrantTable
import domains.user.objects.Username
import shared.objects.access.{GrantRole, ResourceAccessPolicy}

import java.sql.{Connection, Timestamp}
import java.time.Instant

object ProblemMutationTable:

  private val insertSQL: String =
    s"""
      |insert into problems (id, slug, title, statement_text, data_name, data_bytes, base_access, other_user_submission_access, author_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, data_name, ready, base_access, other_user_submission_access, ${UserIdentitySql.returningOptionalColumns("author_username", "author")}, created_at, updated_at
      |""".stripMargin

  def insert(
    connection: Connection,
    problemId: ProblemId,
    createdAt: Instant,
    authorUsername: Username,
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
        statement.setString(7, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setString(8, encodeOtherUserSubmissionAccessColumn(request.otherUserSubmissionAccess))
        statement.setString(9, authorUsername.value)
        statement.setTimestamp(10, Timestamp.from(createdAt))
        statement.setTimestamp(11, Timestamp.from(createdAt))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemDetailBase(resultSet)
          else missingInsertResult("problem")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problem =>
      val sanitizedPolicy = sanitizePolicy(request.accessPolicy)
      ProblemAccessGrantTable
        .replaceForProblem(connection, problem.id, GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        .flatMap(_ =>
          ProblemAccessGrantTable.replaceForProblem(
            connection,
            problem.id,
            GrantRole.Manager,
            sanitizedPolicy.managerGrants
          )
        )
        .as(problem.copy(accessPolicy = sanitizedPolicy))
    }

  private val updateSQL: String =
    """
      |update problems
      |set title = ?, statement_text = ?, base_access = ?, other_user_submission_access = ?, author_username = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def update(connection: Connection, problemId: ProblemId, updatedAt: Instant, request: UpdateProblemRequest): IO[Unit] =
    for
      _ <- IO.blocking {
      val statement = connection.prepareStatement(updateSQL)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.statement.value)
        statement.setString(3, encodeBaseAccessColumn(request.accessPolicy.baseAccess))
        statement.setString(4, encodeOtherUserSubmissionAccessColumn(request.otherUserSubmissionAccess))
        request.authorUsername match
          case Some(username) => statement.setString(5, username.value)
          case None => statement.setNull(5, java.sql.Types.VARCHAR)
        statement.setTimestamp(6, Timestamp.from(updatedAt))
        statement.setObject(7, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
      }
      _ <- ProblemAccessGrantTable.replaceForProblem(connection, problemId, GrantRole.Viewer, request.accessPolicy.viewerGrants)
      _ <- ProblemAccessGrantTable.replaceForProblem(connection, problemId, GrantRole.Manager, request.accessPolicy.managerGrants)
    yield ()

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

  private val lockExistingSQL: String =
    """
      |select 1
      |from problems
      |where id = ?
      |for update
      |""".stripMargin

  def lockExisting(connection: Connection, problemId: ProblemId): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(lockExistingSQL)
      try
        statement.setObject(1, problemId.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val incrementHackRevisionSQL: String =
    """
      |update problems
      |set hack_revision = hack_revision + 1
      |where id = ?
      |""".stripMargin

  def incrementHackRevision(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(incrementHackRevisionSQL)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def sanitizePolicy(policy: ResourceAccessPolicy): ResourceAccessPolicy =
    policy.copy(
      viewerGrants = policy.viewerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject)),
      managerGrants = policy.managerGrants.distinctBy(subject => AccessGrantSql.subjectIdentity(subject))
    )
