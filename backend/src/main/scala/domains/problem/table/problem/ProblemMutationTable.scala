package domains.problem.table.problem

import cats.effect.IO
import database.table.resource_access_grant.ResourceAccessGrantTable
import database.utils.ResourceAccessTableSupport.{encodeBaseAccessColumn, missingInsertResult, sanitizePolicy, toLegacyVisibility}
import database.utils.UserIdentitySql
import domains.problem.objects.request.{CreateProblemRequest, UpdateProblemRequest}
import domains.problem.objects.{ProblemId}
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemTableSupport.*
import domains.user.objects.Username
import shared.objects.access.{GrantRole, ResourceKind}

import java.sql.{Connection, Timestamp}
import java.time.Instant

object ProblemMutationTable:

  private val insertSQL: String =
    s"""
      |insert into problems (id, slug, title, statement_text, data_name, data_bytes, time_limit_ms, space_limit_mb, visibility, base_access, other_user_submission_access, creator_username, created_at, updated_at)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning id, slug, title, statement_text, data_name, ready, time_limit_ms, space_limit_mb, base_access, other_user_submission_access, ${UserIdentitySql.returningColumns("creator_username", "creator")}, created_at, updated_at
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
        statement.setString(11, encodeOtherUserSubmissionAccessColumn(request.otherUserSubmissionAccess))
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
      |set title = ?, statement_text = ?, time_limit_ms = ?, space_limit_mb = ?, visibility = ?, base_access = ?, other_user_submission_access = ?, updated_at = ?
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
        statement.setString(7, encodeOtherUserSubmissionAccessColumn(request.otherUserSubmissionAccess))
        statement.setTimestamp(8, Timestamp.from(updatedAt))
        statement.setObject(9, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
      }
      _ <- ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), GrantRole.Viewer, request.accessPolicy.viewerGrants)
      _ <- ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), GrantRole.Manager, request.accessPolicy.managerGrants)
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
