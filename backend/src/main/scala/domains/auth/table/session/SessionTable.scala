package domains.auth.table.session



import cats.effect.IO
import domains.auth.objects.SessionToken
import domains.user.objects.Username
import domains.auth.table.session.SessionTableSchema.*

import java.sql.Connection
import java.sql.Timestamp
import java.time.{Duration, Instant}

/** auth_sessions 表访问层，负责会话令牌的持久化、续期和删除。 */
object SessionTable:
  /** 数据库中仍未过期的会话记录，包含用户名和过期时间。 */
  final case class ActiveSession(
    username: Username,
    expiresAt: Instant
  )

  /** 初始化会话表结构，并按当前 TTL 回填旧会话的生命周期字段。 */
  def initialize(connection: Connection, sessionTtl: Duration): IO[Unit] =
    SessionTableSchema.initialize(connection, sessionTtl)

  private val insertSQL: String =
    """
      |insert into auth_sessions (token, username, created_at, last_active_at, expires_at)
      |values (?, ?, ?, ?, ?)
      |""".stripMargin

  /** 插入新会话记录，记录创建时间、最后活跃时间和过期时间。 */
  def insert(connection: Connection, token: SessionToken, username: Username, expiresAt: Instant): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setString(1, token.value)
        statement.setString(2, username.value)
        statement.setTimestamp(3, Timestamp.from(now))
        statement.setTimestamp(4, Timestamp.from(now))
        statement.setTimestamp(5, Timestamp.from(expiresAt))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val findSessionByTokenSQL: String =
    """
      |select username, expires_at
      |from auth_sessions
      |where token = ?
      |  and expires_at > ?
      |""".stripMargin

  /** 按令牌查找未过期会话，过期或不存在时返回 None。 */
  def findActiveByToken(
    connection: Connection,
    token: SessionToken,
    now: Instant
  ): IO[Option[ActiveSession]] =
    IO.blocking {
      val statement = connection.prepareStatement(findSessionByTokenSQL)
      try
        statement.setString(1, token.value)
        statement.setTimestamp(2, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ActiveSession(
                username = Username.canonical(resultSet.getString("username")),
                expiresAt = resultSet.getTimestamp("expires_at").toInstant
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val touchSessionSQL: String =
    """
      |update auth_sessions
      |set last_active_at = ?, expires_at = ?
      |where token = ?
      |  and expires_at > ?
      |""".stripMargin

  /** 续期仍未过期的会话，返回是否实际更新了行。 */
  def renewSession(
    connection: Connection,
    token: SessionToken,
    now: Instant,
    nextExpiresAt: Instant
  ): IO[Boolean] =
    IO.blocking {
      val touchStatement = connection.prepareStatement(touchSessionSQL)
      try
        touchStatement.setTimestamp(1, Timestamp.from(now))
        touchStatement.setTimestamp(2, Timestamp.from(nextExpiresAt))
        touchStatement.setString(3, token.value)
        touchStatement.setTimestamp(4, Timestamp.from(now))
        touchStatement.executeUpdate() > 0
      finally touchStatement.close()
    }

  private val findTokensByUsernameSQL: String =
    """
      |select token
      |from auth_sessions
      |where username = ?
      |""".stripMargin

  /** 列出某用户所有会话令牌，用于密码变更后批量清理缓存和数据库。 */
  def findTokensByUsername(connection: Connection, username: Username): IO[List[SessionToken]] =
    IO.blocking {
      val statement = connection.prepareStatement(findTokensByUsernameSQL)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ =>
              SessionToken
                .parse(resultSet.getString("token"))
                .fold(message => throw new IllegalStateException(message), identity)
            )
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteExpiredSQL: String =
    """
      |delete from auth_sessions
      |where expires_at <= ?
      |""".stripMargin

  /** 删除已过期会话，通常在插入新会话前顺带清理。 */
  def deleteExpired(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteExpiredSQL)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteByTokenSQL: String =
    """
      |delete from auth_sessions
      |where token = ?
      |""".stripMargin

  /** 按令牌删除会话，删除不存在的会话也视为成功。 */
  def deleteByToken(connection: Connection, token: SessionToken): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByTokenSQL)
      try
        statement.setString(1, token.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val deleteByUsernameSQL: String =
    """
      |delete from auth_sessions
      |where username = ?
      |""".stripMargin

  /** 删除某用户名下全部会话，常用于密码变更或账号安全操作。 */
  def deleteByUsername(connection: Connection, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByUsernameSQL)
      try
        statement.setString(1, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
