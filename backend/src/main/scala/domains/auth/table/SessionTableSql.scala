package domains.auth.table

object SessionTableSql:

  val insertSql: String =
    """
      |insert into auth_sessions (token, username, created_at, last_active_at, expires_at)
      |values (?, ?, ?, ?, ?)
      |""".stripMargin

  val findSessionByTokenSql: String =
    """
      |select username, expires_at, last_active_at
      |from auth_sessions
      |where token = ?
      |  and expires_at > ?
      |""".stripMargin

  val touchSessionSql: String =
    """
      |update auth_sessions
      |set last_active_at = ?, expires_at = ?
      |where token = ?
      |""".stripMargin

  val deleteByTokenSql: String =
    """
      |delete from auth_sessions
      |where token = ?
      |""".stripMargin

  val deleteByUsernameSql: String =
    """
      |delete from auth_sessions
      |where username = ?
      |""".stripMargin

  val deleteExpiredSql: String =
    """
      |delete from auth_sessions
      |where expires_at <= ?
      |""".stripMargin

  val selectMissingTimestampsSql: String =
    """
      |select token, created_at, last_active_at, expires_at
      |from auth_sessions
      |where last_active_at is null or expires_at is null
      |""".stripMargin

  val backfillTimestampsSql: String =
    """
      |update auth_sessions
      |set last_active_at = ?, expires_at = ?
      |where token = ?
      |""".stripMargin
