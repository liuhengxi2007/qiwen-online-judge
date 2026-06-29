package domains.message.table.message

import cats.effect.IO

import java.sql.Connection

/** 私信 domain 表初始化入口，委托 MessageTableSchema 创建表和索引。 */
object MessageTable:

  /** 初始化私信会话、消息和屏蔽关系表。 */
  def initialize(connection: Connection): IO[Unit] =
    MessageTableSchema.initialize(connection)
