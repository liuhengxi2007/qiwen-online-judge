package domains.message.table.message

import cats.effect.IO
import domains.user.objects.Username

import java.sql.Connection

object MessageUserTable:

  private val userExistsSQL: String =
    """
      |select 1
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  def userExists(connection: Connection, username: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(userExistsSQL)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }
