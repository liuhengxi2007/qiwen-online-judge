package domains.auth.table

object AuthUserTableSql:

  val seedAuthAdminSql: String =
    """
      |insert into auth_users (username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |on conflict (username) do nothing
      |""".stripMargin

  val findAuthUserByUsernameSql: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  val insertAuthUserSql: String =
    """
      |insert into auth_users (username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |""".stripMargin
