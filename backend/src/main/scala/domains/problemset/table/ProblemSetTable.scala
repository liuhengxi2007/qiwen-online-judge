package domains.problemset.table



import database.ResourceAccessGrantTable
import cats.effect.IO
import cats.syntax.all.*
import domains.auth.model.Username
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.problemset.http.request.{CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSet, ProblemSetDescription, ProblemSetId, ProblemSetSlug, ProblemSetTitle}
import domains.problemset.application.view.{ProblemSetProblemSummary, ProblemSetSummary}
import domains.shared.access.{BaseAccess, GrantRole, ResourceAccessPolicy, ResourceId, ResourceKind}
import database.utils.ResourceAccessTableSupport.{missingInsertResult, policyFrom, sanitizePolicy, toLegacyVisibility}
import domains.shared.model.PageResponse
import domains.problemset.table.ProblemSetTableSchema.*
import domains.problemset.table.ProblemSetTableSql.*
import domains.problemset.table.utils.ProblemSetTableSupport.*

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID

object ProblemSetTable:

  enum AddProblemTableResult:
    case AlreadyLinked
    case Linked

  enum RemoveProblemTableResult:
    case NotLinked
    case Removed

  def initialize(connection: Connection): IO[Unit] =
    ProblemSetTableSchema.initialize(connection)

  def listVisibleTo(connection: Connection, actor: domains.auth.model.AuthUser, page: Int, pageSize: Int): IO[PageResponse[ProblemSetSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSql)
        try
          bindVisibilityQuery(statement, actor, None, None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSql)
        try
          bindVisibilityQuery(statement, actor, Some(pageSize), Some((page - 1) * pageSize))
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
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(item.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(item.id), GrantRole.Manager)
        yield item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, viewerGrants, managerGrants))
      }
    yield PageResponse(items = itemsWithPolicies, page = page, pageSize = pageSize, totalItems = totalItems)

  def findBySlug(connection: Connection, slug: ProblemSetSlug): IO[Option[ProblemSet]] =
    IO.blocking {
      val statement = connection.prepareStatement(findBySlugSql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemSetDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(problemSet) =>
        for
          problems <- listProblemsForSet(connection, problemSet.id, listProblemsForSetSql)
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), GrantRole.Manager)
        yield Some(problemSet.copy(problems = problems, accessPolicy = policyFrom(problemSet.accessPolicy.baseAccess, viewerGrants, managerGrants)))
      case None =>
        IO.pure(None)
    }

  def insert(connection: Connection, creatorUsername: Username, request: CreateProblemSetRequest): IO[ProblemSet] =
    IO.blocking {
      val now = Instant.now()
      val problemSetId = ProblemSetId(UUID.randomUUID())
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, problemSetId.value)
        statement.setString(2, request.slug.value)
        statement.setString(3, request.title.value)
        statement.setString(4, request.description.value)
        statement.setString(5, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(6, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setString(7, creatorUsername.value)
        statement.setTimestamp(8, Timestamp.from(now))
        statement.setTimestamp(9, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProblemSetDetailBase(resultSet).copy(problems = Nil)
          else missingInsertResult("problem set")
        finally resultSet.close()
      finally statement.close()
    }.flatMap { problemSet =>
      val sanitizedPolicy = sanitizePolicy(request.accessPolicy)
      ResourceAccessGrantTable
        .replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSet.id), GrantRole.Viewer, sanitizedPolicy.viewerGrants)
        .flatMap(_ =>
          ResourceAccessGrantTable.replaceForResource(
            connection,
            ResourceKind.ProblemSet,
            toResourceId(problemSet.id),
            GrantRole.Manager,
            sanitizedPolicy.managerGrants
          )
        )
        .as(problemSet.copy(accessPolicy = sanitizedPolicy))
    }

  def addProblem(connection: Connection, problemSetId: ProblemSetId, problemId: ProblemId): IO[AddProblemTableResult] =
    for
      alreadyLinked <- IO.blocking {
        val statement = connection.prepareStatement(relationExistsSql)
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
          val nextPositionStatement = connection.prepareStatement(nextPositionSql)
          try
            nextPositionStatement.setObject(1, problemSetId.value)
            val nextPositionResultSet = nextPositionStatement.executeQuery()
            val nextPosition =
              try
                if nextPositionResultSet.next() then nextPositionResultSet.getInt("current_max") + 1
                else 1
              finally nextPositionResultSet.close()

            val insertStatement = connection.prepareStatement(insertRelationSql)
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

  def update(connection: Connection, problemSetId: ProblemSetId, request: UpdateProblemSetRequest): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(updateSql)
      try
        statement.setString(1, request.title.value)
        statement.setString(2, request.description.value)
        statement.setString(3, toLegacyVisibility(request.accessPolicy.baseAccess))
        statement.setString(4, BaseAccess.toDatabase(request.accessPolicy.baseAccess))
        statement.setTimestamp(5, Timestamp.from(now))
        statement.setObject(6, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    } *>
      ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSetId), GrantRole.Viewer, request.accessPolicy.viewerGrants) *>
      ResourceAccessGrantTable.replaceForResource(connection, ResourceKind.ProblemSet, toResourceId(problemSetId), GrantRole.Manager, request.accessPolicy.managerGrants)

  def delete(connection: Connection, problemSetId: ProblemSetId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setObject(1, problemSetId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def removeProblem(connection: Connection, problemSetId: ProblemSetId, problemId: ProblemId): IO[RemoveProblemTableResult] =
    IO.blocking {
      val positionStatement = connection.prepareStatement(findRelationPositionSql)
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
            val deleteStatement = connection.prepareStatement(deleteRelationSql)
            try
              deleteStatement.setObject(1, problemSetId.value)
              deleteStatement.setObject(2, problemId.value)
              deleteStatement.executeUpdate()
            finally deleteStatement.close()

            val compactStatement = connection.prepareStatement(compactPositionsSql)
            try
              compactStatement.setObject(1, problemSetId.value)
              compactStatement.setInt(2, position)
              compactStatement.executeUpdate()
            finally compactStatement.close()

            RemoveProblemTableResult.Removed
      finally positionStatement.close()
    }
