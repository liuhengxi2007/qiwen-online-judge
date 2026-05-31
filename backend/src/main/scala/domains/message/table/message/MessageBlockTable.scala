package domains.message.table.message

import cats.effect.IO
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.MessageTableSupport.*
import domains.user.objects.Username

import java.sql.{Connection, Timestamp}

object MessageBlockTable:

  private val isBlockedSQL: String =
    """
      |select 1
      |from message_blocks
      |where lower(owner_username) = lower(?)
      |  and lower(blocked_username) = lower(?)
      |""".stripMargin

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
