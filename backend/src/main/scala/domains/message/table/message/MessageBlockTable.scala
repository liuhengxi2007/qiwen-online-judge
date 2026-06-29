package domains.message.table.message

import cats.effect.IO
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.MessageTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}

/** 私信屏蔽表访问对象，负责检查、列出、写入和移除屏蔽关系。 */
object MessageBlockTable:

  private val isBlockedSQL: String =
    """
      |select 1
      |from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin

  /** 判断 owner 是否屏蔽了 blocked，用于发送私信前的权限边界。 */
  def isBlocked(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(isBlockedSQL)
      try
        statement.setString(1, ownerUsername.value)
        statement.setString(2, blockedUsername.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }

  private val listBlocksSQL: String =
    """
      |select mb.blocked_username,
      |       up.display_name as blocked_display_name,
      |       mb.created_at
      |from message_blocks mb
      |join user_profiles up on lower(up.username) = lower(mb.blocked_username)
      |where lower(mb.owner_username) = lower(?)
      |order by mb.created_at desc, lower(mb.blocked_username) asc
      |""".stripMargin

  /** 读取当前用户屏蔽名单，按屏蔽时间倒序返回。 */
  def listBlocks(connection: Connection, ownerUsername: Username): IO[List[MessageBlockEntry]] =
    IO.blocking {
      val statement = connection.prepareStatement(listBlocksSQL)
      try
        statement.setString(1, ownerUsername.value)
        val resultSet = statement.executeQuery()
        try Iterator.continually(resultSet.next()).takeWhile(identity).map(_ => readBlockEntry(resultSet)).toList
        finally resultSet.close()
      finally statement.close()
    }

  private val upsertBlockSQL: String =
    """
      |insert into message_blocks (owner_username, blocked_username, created_at)
      |values (?, ?, ?)
      |on conflict (owner_username, blocked_username)
      |do update set created_at = excluded.created_at
      |""".stripMargin

  private val readBlockEntrySQL: String =
    """
      |select mb.blocked_username,
      |       up.display_name as blocked_display_name,
      |       mb.created_at
      |from message_blocks mb
      |join user_profiles up on lower(up.username) = lower(mb.blocked_username)
      |where lower(mb.owner_username) = lower(?)
      |  and lower(mb.blocked_username) = lower(?)
      |""".stripMargin

  /** 创建或刷新屏蔽关系，并重读被屏蔽用户身份用于响应。 */
  def upsertBlock(
    connection: Connection,
    ownerUsername: Username,
    blockedUsername: Username
  ): IO[MessageBlockEntry] =
    for
      now <- IO.realTimeInstant
      _ <- IO.blocking {
        val statement = connection.prepareStatement(upsertBlockSQL)
        try
          statement.setString(1, ownerUsername.value)
          statement.setString(2, blockedUsername.value)
          statement.setTimestamp(3, Timestamp.from(now))
          statement.executeUpdate()
          ()
        finally statement.close()
      }
      entry <- IO.blocking {
        val statement = connection.prepareStatement(readBlockEntrySQL)
        try
          statement.setString(1, ownerUsername.value)
          statement.setString(2, blockedUsername.value)
          val resultSet = statement.executeQuery()
          try
            /** 注意：屏蔽关系刚刚 upsert；重读失败表示事务内数据不变量被破坏。 */
            if resultSet.next() then readBlockEntry(resultSet)
            else throw IllegalStateException("Inserted block entry is missing.")
          finally resultSet.close()
        finally statement.close()
      }
    yield entry

  private val removeBlockSQL: String =
    """
      |delete from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin

  /** 删除屏蔽关系；没有匹配记录时保持幂等成功。 */
  def removeBlock(connection: Connection, ownerUsername: Username, blockedUsername: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(removeBlockSQL)
      try
        statement.setString(1, ownerUsername.value)
        statement.setString(2, blockedUsername.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
