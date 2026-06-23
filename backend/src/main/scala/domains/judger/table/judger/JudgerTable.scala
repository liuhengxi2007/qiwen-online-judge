package domains.judger.table.judger



import cats.effect.IO
import domains.judger.objects.response.RegisteredJudgerListItem
import judgeprotocol.objects.{JudgerId, SubmissionLanguage}
import judgeprotocol.objects.request.RegisterJudgerRequest
import judgeprotocol.objects.response.RegisterJudgerResponse
import domains.judger.table.judger.JudgerTableSchema.*
import domains.judger.table.judger.JudgerTableSupport.*

import java.sql.{Connection, Timestamp}
import java.time.Instant

/** judgers 表读写入口；维护 worker 注册、心跳租约和支持语言。 */
object JudgerTable:

  /** 初始化 judger 注册表。 */
  def initialize(connection: Connection): IO[Unit] =
    JudgerTableSchema.initialize(connection)

  private val deleteExpiredSQL: String =
    """
      |delete from judgers
      |where last_heartbeat_at < ?
      |""".stripMargin

  private def deleteExpired(connection: java.sql.Connection, cutoff: Instant): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteExpiredSQL)
      try
        statement.setTimestamp(1, Timestamp.from(cutoff))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val lockJudgerPrefixSQL: String =
    "select pg_advisory_xact_lock(hashtext(?)::bigint)"

  private def lockJudgerPrefix(connection: java.sql.Connection, preferredPrefix: JudgerId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(lockJudgerPrefixSQL)
      try
        statement.setString(1, s"judger-prefix:${preferredPrefix.value}")
        statement.execute()
        ()
      finally statement.close()
    }

  private val listAllocatedIdsSQL: String =
    """
      |select judger_id
      |from judgers
      |where requested_prefix = ?
      |order by judger_id asc
      |""".stripMargin

  private def allocateJudgerId(connection: java.sql.Connection, preferredPrefix: JudgerId): IO[JudgerId] =
    IO.blocking {
      val statement = connection.prepareStatement(listAllocatedIdsSQL)
      try
        statement.setString(1, preferredPrefix.value)
        val resultSet = statement.executeQuery()
        try
          val allocatedIds =
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => resultSet.getString("judger_id"))
              .toSet

          Iterator
            .from(1)
            .map { index =>
              if index == 1 then preferredPrefix.value
              else s"${preferredPrefix.value}-$index"
            }
            .find(id => !allocatedIds.contains(id))
            .flatMap(JudgerId.parse(_).toOption)
            .getOrElse(throw new IllegalStateException("Failed to allocate judger id."))
        finally resultSet.close()
      finally statement.close()
    }

  private val insertSQL: String =
    """
      |insert into judgers (judger_id, requested_prefix, host, process_id, supported_languages, registered_at, last_heartbeat_at)
      |values (?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  /** 注册 judger 并分配唯一 id；注册前会删除已过期 worker 记录。 */
  def register(
    connection: Connection,
    request: RegisterJudgerRequest,
    heartbeatIntervalMs: Long,
    heartbeatTimeoutMs: Long
  ): IO[RegisterJudgerResponse] =
    for
      now <- IO.realTimeInstant
      _ <- deleteExpired(connection, now.minusMillis(heartbeatTimeoutMs))
      _ <- lockJudgerPrefix(connection, request.preferredPrefix)
      allocatedId <- allocateJudgerId(connection, request.preferredPrefix)
      _ <- IO.blocking {
        val statement = connection.prepareStatement(insertSQL)
        try
          statement.setString(1, allocatedId.value)
          statement.setString(2, request.preferredPrefix.value)
          statement.setString(3, request.host)
          request.processId match
            case Some(processId) => statement.setString(4, processId)
            case None => statement.setNull(4, java.sql.Types.VARCHAR)
          statement.setString(5, request.supportedLanguages.map(SubmissionLanguage.render).mkString(","))
          statement.setTimestamp(6, Timestamp.from(now))
          statement.setTimestamp(7, Timestamp.from(now))
          statement.executeUpdate()
          ()
        finally statement.close()
      }
    yield RegisterJudgerResponse(
      judgerId = allocatedId,
      heartbeatIntervalMs = heartbeatIntervalMs,
      heartbeatTimeoutMs = heartbeatTimeoutMs
    )

  private val heartbeatSQL: String =
    """
      |update judgers
      |set last_heartbeat_at = ?
      |where judger_id = ?
      |  and last_heartbeat_at >= ?
      |""".stripMargin

  /** 记录 judger 心跳；只有未过期记录会被更新，返回是否成功续约。 */
  def heartbeat(connection: Connection, judgerId: JudgerId, heartbeatTimeoutMs: Long): IO[Boolean] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(heartbeatSQL)
      try
        statement.setTimestamp(1, Timestamp.from(now))
        statement.setString(2, judgerId.value)
        statement.setTimestamp(3, Timestamp.from(now.minusMillis(heartbeatTimeoutMs)))
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val findActiveSupportedLanguagesSQL: String =
    """
      |select supported_languages
      |from judgers
      |where judger_id = ?
      |  and last_heartbeat_at >= ?
      |""".stripMargin

  /** 查询活动 judger 支持语言；过期或不存在时返回 None。 */
  def findActiveSupportedLanguages(
    connection: Connection,
    judgerId: JudgerId,
    heartbeatTimeoutMs: Long
  ): IO[Option[List[SubmissionLanguage]]] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(findActiveSupportedLanguagesSQL)
      try
        statement.setString(1, judgerId.value)
        statement.setTimestamp(2, Timestamp.from(now.minusMillis(heartbeatTimeoutMs)))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(parseSupportedLanguages(resultSet.getString("supported_languages")))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val listJudgersSQL: String =
    """
      |select judger_id, requested_prefix, host, process_id, supported_languages, registered_at, last_heartbeat_at
      |from judgers
      |order by last_heartbeat_at desc, judger_id asc
      |""".stripMargin

  /** 清理过期记录后列出剩余 judger，按最近心跳倒序。 */
  def listJudgers(connection: Connection, heartbeatTimeoutMs: Long): IO[List[RegisteredJudgerListItem]] =
    for
      now <- IO.realTimeInstant
      _ <- deleteExpired(connection, now.minusMillis(heartbeatTimeoutMs))
      judgers <- IO.blocking {
        val statement = connection.prepareStatement(listJudgersSQL)
        try
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readRegisteredJudgerListItem(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield judgers
