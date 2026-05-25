package domains.blog.table.blog

import cats.effect.IO

import java.sql.Connection

object BlogTable:

  def initialize(connection: Connection): IO[Unit] =
    BlogTableSchema.initialize(connection)
