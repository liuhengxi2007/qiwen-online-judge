package domains.hack.table.hack

import cats.effect.IO

import java.sql.Connection

object HackTable:
  def initialize(connection: Connection): IO[Unit] =
    HackTableSchema.initialize(connection)
