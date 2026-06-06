package domains.hack.table.hack

import database.utils.UserIdentitySql
import domains.hack.objects.HackStatus
import domains.user.objects.{DisplayName, UserIdentity, Username}

import java.sql.ResultSet

object HackTableSupport:

  def encodeHackStatusColumn(value: HackStatus): String =
    HackStatus.encode(value)

  def decodeHackStatusColumn(value: String): HackStatus =
    HackStatus.parse(value).fold(message => throw IllegalStateException(s"Invalid hack status: $message"), identity)

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )
