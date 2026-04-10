package domains.auth.table

import domains.auth.application.PasswordHasher
import cats.effect.IO
import domains.auth.model.{
  AuthSeedUser,
  AuthUser,
  AuthUserListItem,
  DisplayName,
  EmailAddress,
  PasswordHash,
  PlaintextPassword,
  SiteManagerUser,
  Username
}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}

object AuthUserTable:

  private val logger = Slf4jLogger.getLogger[IO]

  private val seedAdminUser = AuthSeedUser(
    username = Username.canonical("admin"),
    displayName = DisplayName("Admin User"),
    email = EmailAddress("admin@example.com"),
    password = PlaintextPassword("password123"),
    siteManager = true,
    problemManager = true
  )

  val initTableSql: String =
    """
      |create table if not exists auth_users (
      |  username varchar(120) primary key,
      |  display_name varchar(120) not null,
      |  email varchar(255) not null,
      |  password_hash varchar(255) not null,
      |  site_manager boolean not null default false,
      |  problem_manager boolean not null default false
      |);
      |""".stripMargin

  val migrateEmailColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'email'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'username'
      |  ) then
      |    alter table auth_users rename column email to username;
      |  end if;
      |end $$;
      |""".stripMargin

  val migratePasswordColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'password'
      |  ) then
      |    alter table auth_users rename column password to password_hash;
      |  end if;
      |end $$;
      |""".stripMargin

  val seedAdminSql: String =
    """
      |insert into auth_users (username, display_name, email, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?)
      |on conflict (username) do update
      |set display_name = excluded.display_name,
      |    email = excluded.email,
      |    password_hash = excluded.password_hash,
      |    site_manager = excluded.site_manager,
      |    problem_manager = excluded.problem_manager
      |""".stripMargin

  val addEmailColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'email'
      |  ) then
      |    alter table auth_users add column email varchar(255);
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillEmailSql: String =
    """
      |update auth_users
      |set email = username || '@example.com'
      |where email is null or btrim(email) = ''
      |""".stripMargin

  val setEmailNotNullSql: String =
    """
      |alter table auth_users
      |alter column email set not null
      |""".stripMargin

  val createCaseInsensitiveUsernameIndexSql: String =
    """
      |create index if not exists auth_users_username_idx
      |on auth_users (username)
      |""".stripMargin

  val addSiteManagerColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'site_manager'
      |  ) then
      |    alter table auth_users add column site_manager boolean not null default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val addProblemManagerColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'problem_manager'
      |  ) then
      |    alter table auth_users add column problem_manager boolean not null default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val findByUsernameSql: String =
    """
      |select username, display_name, email, password_hash, site_manager, problem_manager
      |from auth_users
      |where username = ?
      |""".stripMargin

  val listUsersSql: String =
    """
      |select username, display_name, email, site_manager, problem_manager
      |from auth_users
      |order by username asc
      |""".stripMargin

  val updatePermissionsSql: String =
    """
      |update auth_users
      |set site_manager = ?, problem_manager = ?
      |where username = ?
      |returning username, display_name, email, password_hash, site_manager, problem_manager
      |""".stripMargin

  val updateOwnSettingsSql: String =
    """
      |update auth_users
      |set display_name = ?, email = ?, password_hash = ?
      |where username = ?
      |returning username, display_name, email, password_hash, site_manager, problem_manager
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from auth_users
      |where username = ?
      |""".stripMargin

  enum DeleteUserTableResult:
    case NotFound
    case Deleted
    case HasOwnedResources

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.createStatement()
        try
          statement.execute(migrateEmailColumnSql)
          statement.execute(migratePasswordColumnSql)
          statement.execute(initTableSql)
          statement.execute(addEmailColumnSql)
          statement.execute(addSiteManagerColumnSql)
          statement.execute(addProblemManagerColumnSql)
          statement.executeUpdate(backfillEmailSql)
          statement.execute(setEmailNotNullSql)
          statement.execute(createCaseInsensitiveUsernameIndexSql)
        finally statement.close()
      }
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
    password: PlaintextPassword
  ): IO[AuthUser] =
    for
      passwordHash <- PasswordHasher.hashPassword(password)
      user <- IO.blocking {
        val statement = connection.prepareStatement(
          """
            |insert into auth_users (username, display_name, email, password_hash, site_manager, problem_manager)
            |values (?, ?, ?, ?, ?, ?)
            |returning username, display_name, email, password_hash, site_manager, problem_manager
            |""".stripMargin
        )
        try
          statement.setString(1, username.value.trim)
          statement.setString(2, displayName.value.trim)
          statement.setString(3, email.value.trim)
          statement.setString(4, passwordHash.value)
          statement.setBoolean(5, false)
          statement.setBoolean(6, false)

          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then readAuthUser(resultSet)
            else missingInsertResult("user")
          finally resultSet.close()
        finally statement.close()
      }
    yield user

  private def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

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
    passwordHash: PasswordHash
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateOwnSettingsSql)
      try
        statement.setString(1, displayName.value.trim)
        statement.setString(2, email.value.trim)
        statement.setString(3, passwordHash.value)
        statement.setString(4, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private def seedAdmin(connection: Connection): IO[Unit] =
    for
      passwordHash <- PasswordHasher.hashPassword(seedAdminUser.password)
      _ <- IO.blocking {
        val statement = connection.prepareStatement(seedAdminSql)
        try
          statement.setString(1, seedAdminUser.username.value)
          statement.setString(2, seedAdminUser.displayName.value)
          statement.setString(3, seedAdminUser.email.value)
          statement.setString(4, passwordHash.value)
          statement.setBoolean(5, seedAdminUser.siteManager)
          statement.setBoolean(6, seedAdminUser.problemManager)
          statement.executeUpdate()
        finally statement.close()
      }
      _ <- logger.info(s"Ensured seeded auth user exists, username=${seedAdminUser.username.value}")
    yield ()

  private def readAuthUser(resultSet: ResultSet): AuthUser =
    AuthUser(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      email = EmailAddress(resultSet.getString("email")),
      passwordHash = PasswordHash(resultSet.getString("password_hash")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )
