package domains.auth.table

import domains.auth.application.PasswordHasher
import cats.effect.IO
import domains.auth.model.{
  AuthSeedUser,
  AuthUser,
  DisplayName,
  EmailAddress,
  PasswordHash,
  PlaintextPassword,
  SiteManagerUser,
  UserDisplayMode,
  UserLocale,
  Username
}
import domains.problem.model.{ProblemSlug, ProblemTitle, ProblemTitleDisplayMode}
import domains.shared.model.{PageRequest, PageResponse}
import domains.auth.table.AuthUserTableSchema.*
import domains.auth.table.AuthUserTableSql.*
import domains.auth.table.AuthUserTableSupport.*
import domains.user.model.{AuthUserListItem, UserAcceptedProblem, UserAcceptedRanklistItem, UserContribution, UserIdentity, UserRanklistItem}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}

object AuthUserTable:

  private val logger = Slf4jLogger.getLogger[IO]

  enum DeleteUserTableResult:
    case NotFound
    case Deleted
    case HasOwnedResources

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- AuthUserTableSchema.initializeSchema(connection)
      _ <- seedAdmin(connection)
    yield ()

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

  def insert(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    email: EmailAddress,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    password: PlaintextPassword
  ): IO[AuthUser] =
    for
      passwordHash <- PasswordHasher.hashPassword(password)
      user <- IO.blocking {
        val statement = connection.prepareStatement(insertSql)
        try
          statement.setString(1, username.value.trim)
          statement.setString(2, displayName.value.trim)
          statement.setString(3, email.value.trim)
          statement.setString(4, UserDisplayMode.toDatabase(displayMode))
          statement.setString(5, UserLocale.toDatabase(locale))
          statement.setString(6, ProblemTitleDisplayMode.toDatabase(problemTitleDisplayMode))
          statement.setString(7, passwordHash.value)
          statement.setBoolean(8, false)
          statement.setBoolean(9, false)

          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then readAuthUser(resultSet)
            else missingInsertResult("user")
          finally resultSet.close()
        finally statement.close()
      }
    yield user

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
            .map(_ =>
              AuthUserListItem(
                username = Username.canonical(resultSet.getString("username")),
                displayName = DisplayName(resultSet.getString("display_name")),
                email = EmailAddress(resultSet.getString("email")),
                siteManager = resultSet.getBoolean("site_manager"),
                problemManager = resultSet.getBoolean("problem_manager")
              )
            )
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
            .map(_ =>
              UserAcceptedProblem(
                slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
                title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
                acceptedAt = resultSet.getTimestamp("accepted_at").toInstant
              )
            )
            .toList
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
      val statement = connection.prepareStatement(updateOwnSettingsSql)
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

  private def countUsers(connection: Connection): Long =
    val statement = connection.prepareStatement(countUsersSql)
    try
      val resultSet = statement.executeQuery()
      try
        if resultSet.next() then resultSet.getLong("total_items")
        else 0L
      finally resultSet.close()
    finally statement.close()

  private def readRanklistItem(resultSet: ResultSet): UserRanklistItem =
    UserRanklistItem(
      user = readUserIdentity(resultSet),
      contribution = UserContribution(BigDecimal(resultSet.getBigDecimal("contribution")))
    )

  private def readAcceptedRanklistItem(resultSet: ResultSet): UserAcceptedRanklistItem =
    UserAcceptedRanklistItem(
      user = readUserIdentity(resultSet),
      acceptedCount = resultSet.getInt("accepted_count")
    )

  private def readUserIdentity(resultSet: ResultSet): UserIdentity =
    UserIdentity(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name"))
    )

  private def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)
