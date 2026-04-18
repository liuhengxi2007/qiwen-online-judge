package domains.auth.table

object AuthUserTableSql:

  val seedAdminSql: String =
    """
      |insert into auth_users (username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |on conflict (username) do update
      |set display_name = excluded.display_name,
      |    email = excluded.email,
      |    display_mode = excluded.display_mode,
      |    locale = excluded.locale,
      |    problem_title_display_mode = excluded.problem_title_display_mode,
      |    password_hash = excluded.password_hash,
      |    site_manager = excluded.site_manager,
      |    problem_manager = excluded.problem_manager
      |""".stripMargin

  val findByUsernameSql: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |from auth_users
      |where username = ?
      |""".stripMargin

  val insertSql: String =
    """
      |insert into auth_users (username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |""".stripMargin

  val listUsersSql: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, site_manager, problem_manager
      |from auth_users
      |order by username asc
      |""".stripMargin

  val updatePermissionsSql: String =
    """
      |update auth_users
      |set site_manager = ?, problem_manager = ?
      |where username = ?
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |""".stripMargin

  val updateOwnSettingsSql: String =
    """
      |update auth_users
      |set display_name = ?, email = ?, display_mode = ?, locale = ?, problem_title_display_mode = ?, password_hash = ?
      |where username = ?
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, password_hash, site_manager, problem_manager
      |""".stripMargin

  val deleteSql: String =
    """
      |delete from auth_users
      |where username = ?
      |""".stripMargin
