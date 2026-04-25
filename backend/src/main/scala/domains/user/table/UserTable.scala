package domains.user.table

import cats.effect.IO
import domains.auth.model.{AuthUser, DisplayName, EmailAddress, PasswordHash, SiteManagerUser, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.shared.model.{PageRequest, PageResponse}
import domains.user.model.{AuthUserListItem, UserAcceptedProblem, UserAcceptedRanklistItem, UserDisplayMode, UserLocale, UserRanklistItem}
import domains.user.table.UserTableSql.*
import domains.user.table.UserTableSupport.*

import java.sql.{Connection, SQLException}

object UserTable:

  enum DeleteUserTableResult:
    case NotFound
    case Deleted
    case HasOwnedResources

  def findByUsername(connection: Connection, username: Username): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByUsernameSql)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def listUsers(connection: Connection, actor: SiteManagerUser): IO[List[AuthUserListItem]] =
    IO.blocking {
      val _ = actor
      val statement = connection.prepareStatement(listUsersSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readUserListItem(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def listContributionRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[UserRanklistItem]] =
    IO.blocking {
      val normalizedPageRequest = pageRequest.normalized
      val totalItems = countUsers(connection)
      val statement = connection.prepareStatement(listContributionRanklistSql)
      try
        statement.setInt(1, normalizedPageRequest.pageSize)
        statement.setInt(2, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val items = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readRanklistItem(resultSet))
            .toList

          PageResponse(
            items = items,
            page = normalizedPageRequest.page,
            pageSize = normalizedPageRequest.pageSize,
            totalItems = totalItems
          )
        finally resultSet.close()
      finally statement.close()
    }

  def listAcceptedRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[UserAcceptedRanklistItem]] =
    IO.blocking {
      val normalizedPageRequest = pageRequest.normalized
      val totalItems = countUsers(connection)
      val statement = connection.prepareStatement(listAcceptedRanklistSql)
      try
        statement.setInt(1, normalizedPageRequest.pageSize)
        statement.setInt(2, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val items = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readAcceptedRanklistItem(resultSet))
            .toList

          PageResponse(
            items = items,
            page = normalizedPageRequest.page,
            pageSize = normalizedPageRequest.pageSize,
            totalItems = totalItems
          )
        finally resultSet.close()
      finally statement.close()
    }

  def listAcceptedProblems(connection: Connection, username: Username): IO[List[UserAcceptedProblem]] =
    IO.blocking {
      val statement = connection.prepareStatement(listAcceptedProblemsSql)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readAcceptedProblem(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def updatePermissions(
    connection: Connection,
    actor: SiteManagerUser,
    username: Username,
    siteManager: Boolean,
    problemManager: Boolean
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val _ = actor
      val statement = connection.prepareStatement(updatePermissionsSql)
      try
        statement.setBoolean(1, siteManager)
        statement.setBoolean(2, problemManager)
        statement.setString(3, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def updateSettings(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    email: EmailAddress,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    passwordHash: PasswordHash
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateSettingsSql)
      try
        statement.setString(1, displayName.value.trim)
        statement.setString(2, email.value.trim)
        statement.setString(3, UserDisplayMode.toDatabase(displayMode))
        statement.setString(4, UserLocale.toDatabase(locale))
        statement.setString(5, ProblemTitleDisplayMode.toDatabase(problemTitleDisplayMode))
        statement.setString(6, passwordHash.value)
        statement.setString(7, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def delete(connection: Connection, username: Username): IO[DeleteUserTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setString(1, username.value)
        try
          val deletedRows = statement.executeUpdate()
          if deletedRows == 0 then DeleteUserTableResult.NotFound
          else DeleteUserTableResult.Deleted
        catch
          case exception: SQLException if exception.getSQLState == "23503" =>
            DeleteUserTableResult.HasOwnedResources
      finally statement.close()
    }

  private def countUsers(connection: Connection): Long =
    val statement = connection.prepareStatement(countUsersSql)
    try
      val resultSet = statement.executeQuery()
      try
        if resultSet.next() then resultSet.getLong("total_items")
        else 0L
      finally resultSet.close()
    finally statement.close()
