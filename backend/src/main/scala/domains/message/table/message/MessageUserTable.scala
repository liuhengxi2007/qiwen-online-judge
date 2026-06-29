package domains.message.table.message

import cats.effect.IO
import domains.user.objects.Username

import java.sql.Connection

/** 私信用户辅助表访问对象，用于校验目标账号是否存在。 */
object MessageUserTable:

  private val userExistsSQL: String =
    """
      |select 1
      |from auth_accounts
      |where lower(username) = lower(?)
      |""".stripMargin

  /** 按大小写不敏感用户名检查账号存在性。 */
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
