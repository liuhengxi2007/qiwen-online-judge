package tables

import auth.PasswordHasher
import cats.effect.IO
import cats.syntax.all.*
import objects.{AuthUser, AuthUserListItem}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.{Connection, PreparedStatement, ResultSet}

object AuthUserTable:

  private val logger = Slf4jLogger.getLogger[IO]

  private val seedAdminUser = AuthUser(
    username = "admin",
    displayName = "Admin User",
    email = "admin@example.com",
    passwordHash = PasswordHasher.hashPassword("password123")
  )

  val initTableSql: String =
    """
      |create table if not exists auth_users (
      |  username varchar(120) primary key,
      |  display_name varchar(120) not null,
      |  email varchar(255) not null,
      |  password_hash varchar(255) not null
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
      |insert into auth_users (username, display_name, email, password_hash)
      |values (?, ?, ?, ?)
      |on conflict (username) do update
      |set display_name = excluded.display_name,
      |    email = excluded.email,
      |    password_hash = excluded.password_hash
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
      |create unique index if not exists auth_users_username_lower_uidx
      |on auth_users (lower(username))
      |""".stripMargin

  val migrateSeedAdminUsernameSql: String =
    """
      |update auth_users
      |set username = 'admin'
      |where username = 'admin@example.com'
      |  and not exists (
      |    select 1
      |    from auth_users existing_user
      |    where existing_user.username = 'admin'
      |  )
      |""".stripMargin

  val deleteLegacySeedAdminSql: String =
    """
      |delete from auth_users
      |where username = 'admin@example.com'
      |""".stripMargin

  val findByUsernameSql: String =
    """
      |select username, display_name, email, password_hash
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  val listUsersSql: String =
    """
      |select username, display_name, email
      |from auth_users
      |order by lower(username) asc
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.createStatement()
        try
          statement.execute(migrateEmailColumnSql)
          statement.execute(migratePasswordColumnSql)
          statement.execute(initTableSql)
          statement.execute(addEmailColumnSql)
          statement.executeUpdate(backfillEmailSql)
          statement.execute(setEmailNotNullSql)
          statement.execute(createCaseInsensitiveUsernameIndexSql)
          statement.executeUpdate(migrateSeedAdminUsernameSql)
          statement.executeUpdate(deleteLegacySeedAdminSql)
        finally statement.close()
      }
      _ <- migratePlaintextPasswords(connection)
      _ <- seedAdmin(connection)
    yield ()

  def findByUsername(connection: Connection, username: String): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByUsernameSql)
      try
        statement.setString(1, username.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def insert(
    connection: Connection,
    username: String,
    displayName: String,
    email: String,
    password: String
  ): IO[AuthUser] =
    IO.blocking {
      val statement = connection.prepareStatement(
        """
          |insert into auth_users (username, display_name, email, password_hash)
          |values (?, ?, ?, ?)
          |returning username, display_name, email, password_hash
          |""".stripMargin
      )
      try
        statement.setString(1, username.trim)
        statement.setString(2, displayName.trim)
        statement.setString(3, email.trim)
        statement.setString(4, PasswordHasher.hashPassword(password))

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readAuthUser(resultSet)
          else throw new IllegalStateException("Insert succeeded but returned no user")
        finally resultSet.close()
      finally statement.close()
    }

  def listUsers(connection: Connection): IO[List[AuthUserListItem]] =
    IO.blocking {
      val statement = connection.prepareStatement(listUsersSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ =>
              AuthUserListItem(
                username = resultSet.getString("username"),
                displayName = resultSet.getString("display_name"),
                email = resultSet.getString("email")
              )
            )
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private def seedAdmin(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(seedAdminSql)
      try
        statement.setString(1, seedAdminUser.username)
        statement.setString(2, seedAdminUser.displayName)
        statement.setString(3, seedAdminUser.email)
        statement.setString(4, seedAdminUser.passwordHash)
        statement.executeUpdate()
      finally statement.close()
    }.flatTap(_ =>
      logger.info(s"Ensured seeded auth user exists, username=${seedAdminUser.username}")
    ).void

  private def migratePlaintextPasswords(connection: Connection): IO[Unit] =
    for
      usersNeedingMigration <- IO.blocking {
        val statement = connection.prepareStatement(
          """
            |select username, display_name, password_hash
            |     , email
            |from auth_users
            |where password_hash not like 'pbkdf2-sha256$%'
            |""".stripMargin
        )
        try
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readAuthUser(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      _ <- usersNeedingMigration.traverse_ { user =>
        val hashedPassword = PasswordHasher.hashPassword(user.passwordHash)
        IO.blocking {
          val statement = connection.prepareStatement(
            """
              |update auth_users
              |set password_hash = ?
              |where username = ?
              |""".stripMargin
          )
          try
            statement.setString(1, hashedPassword)
            statement.setString(2, user.username)
            statement.executeUpdate()
          finally statement.close()
        }
      }
      _ <-
        if usersNeedingMigration.nonEmpty then
          logger.info(s"Migrated plaintext passwords to password hashes, count=${usersNeedingMigration.size}")
        else IO.unit
    yield ()

  private def readAuthUser(resultSet: ResultSet): AuthUser =
    AuthUser(
      username = resultSet.getString("username"),
      displayName = resultSet.getString("display_name"),
      email = resultSet.getString("email"),
      passwordHash = resultSet.getString("password_hash")
    )
