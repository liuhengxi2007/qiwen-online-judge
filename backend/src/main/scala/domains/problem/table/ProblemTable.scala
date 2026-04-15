package domains.problem.table

import cats.effect.IO
import domains.auth.model.Username
import domains.problem.model.{CreateProblemRequest, OthersSubmissionAccess, ProblemData, ProblemDataFilename, ProblemDetail, ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemSummary, ProblemTimeLimitMs, ProblemTitle, UpdateProblemRequest}
import domains.shared.access.{AccessSubject, BaseAccess, GrantRole, ResourceAccessGrant, ResourceAccessGrantTable, ResourceAccessPolicy, ResourceId, ResourceKind}
import domains.shared.model.PageResponse
import domains.problem.table.ProblemTableSchema.*
import domains.problem.table.ProblemTableSql.*
import domains.problem.table.ProblemTableSupport.*

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant

object ProblemTable:

  def initialize(connection: Connection): IO[Unit] =
    ProblemTableSchema.initialize(connection)

  def listVisibleTo(connection: Connection, actor: domains.auth.model.AuthUser, page: Int, pageSize: Int): IO[PageResponse[ProblemSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSql)
        try
          bindVisibilityQuery(statement, actor, pageSize = None, offset = None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSql)
        try
          bindVisibilityQuery(statement, actor, pageSize = Some(pageSize), offset = Some((page - 1) * pageSize))
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
      itemsWithPolicies <- items.foldLeft(IO.pure(List.empty[ProblemSummary])) { (accIO, item) =>
        for
          acc <- accIO
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id), GrantRole.Manager)
        yield acc :+ item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, viewerGrants, managerGrants))
      }
    yield PageResponse(items = itemsWithPolicies, page = page, pageSize = pageSize, totalItems = totalItems)

  def findBySlug(connection: Connection, slug: ProblemSlug): IO[Option[ProblemDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSql)
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

  def hasVisibleContainingProblemSet(
    connection: Connection,
    actor: domains.auth.model.AuthUser,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasVisibleContainingProblemSetSql)
      try
        bindContainingProblemSetVisibilityQuery(statement, actor, problemId)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  def insert(connection: Connection, creatorUsername: Username, request: CreateProblemRequest): IO[ProblemDetail] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, ProblemId.random().value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.statement.value)
        statement.setNull(5, java.sql.Types.VARCHAR)
        statement.setNull(6, java.sql.Types.BINARY)
        statement.setInt(7, request.timeLimitMs.value)
        statement.setInt(8, request.spaceLimitMb.value)
        statement.setString(9, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(10, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(11, OthersSubmissionAccess.toDatabase(request.othersSubmissionAccess))
        statement.setString(12, creatorUsername.value)
        statement.setTimestamp(13, Timestamp.from(now))
        statement.setTimestamp(14, Timestamp.from(now))
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

  def update(connection: Connection, problemId: ProblemId, request: UpdateProblemRequest): IO[Unit] =
    for
      _ <- IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSql)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.statement.value)
        statement.setInt(3, request.timeLimitMs.value)
        statement.setInt(4, request.spaceLimitMb.value)
        statement.setString(5, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(6, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(7, OthersSubmissionAccess.toDatabase(request.othersSubmissionAccess))
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setObject(9, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
      }
      _ <- ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), GrantRole.Viewer, request.accessPolicy.viewerGrants)
      _ <- ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.Problem, toResourceId(problemId), GrantRole.Manager, request.accessPolicy.managerGrants)
    yield ()

  def updateData(connection: Connection, problemId: ProblemId, filename: ProblemDataFilename): IO[Unit] =
    updateData(connection, problemId, Some(filename))

  def updateData(connection: Connection, problemId: ProblemId, filename: Option[ProblemDataFilename]): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateDataSql)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setTimestamp(2, Timestamp.from(now))
        statement.setObject(3, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def delete(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setObject(1, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
