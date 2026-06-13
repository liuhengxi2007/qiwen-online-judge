package domains.hack.table.hack

import database.utils.UserIdentitySql
import domains.hack.objects.HackStatus
import domains.user.objects.{DisplayName, UserIdentity, Username}

import java.sql.ResultSet

/** hack 表读写辅助；集中处理状态列编解码和用户身份映射。 */
object HackTableSupport:

  /** 将 hack 状态编码为数据库列值。 */
  def encodeHackStatusColumn(value: HackStatus): String =
    HackStatus.encode(value)

  /** 从数据库列解析 hack 状态；非法值视为数据损坏。 */
  def decodeHackStatusColumn(value: String): HackStatus =
    HackStatus.parse(value).fold(message => throw IllegalStateException(s"Invalid hack status: $message"), identity)

  /** 从带前缀的 ResultSet 列读取用户身份。 */
  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )
