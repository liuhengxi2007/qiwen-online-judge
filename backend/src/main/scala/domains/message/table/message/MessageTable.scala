package domains.message.table.message

import cats.effect.IO

import java.sql.Connection

object MessageTable:

  def initialize(connection: Connection): IO[Unit] =
    MessageTableSchema.initialize(connection)
