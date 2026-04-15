package domains.judger.table

object JudgerTableSql:

  val deleteExpiredSql: String =
    """
      |delete from judgers
      |where last_heartbeat_at < ?
      |""".stripMargin

  val listAllocatedIdsSql: String =
    """
      |select judger_id
      |from judgers
      |where requested_prefix = ?
      |order by judger_id asc
      |""".stripMargin

  val insertSql: String =
    """
      |insert into judgers (judger_id, requested_prefix, host, process_id, supported_languages, registered_at, last_heartbeat_at)
      |values (?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  val heartbeatSql: String =
    """
      |update judgers
      |set last_heartbeat_at = ?
      |where judger_id = ?
      |  and last_heartbeat_at >= ?
      |""".stripMargin

  val listJudgersSql: String =
    """
      |select judger_id, requested_prefix, host, process_id, supported_languages, registered_at, last_heartbeat_at
      |from judgers
      |order by last_heartbeat_at desc, judger_id asc
      |""".stripMargin
