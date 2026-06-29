package domains.hack.table.hack

import cats.effect.IO

import java.sql.Connection

/** hack 域表初始化入口。 */
object HackTable:
  /** 创建或迁移 hack_attempts 表结构。 */
  def initialize(connection: Connection): IO[Unit] =
    HackTableSchema.initialize(connection)
