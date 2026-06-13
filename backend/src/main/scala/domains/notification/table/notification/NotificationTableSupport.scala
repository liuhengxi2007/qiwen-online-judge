package domains.notification.table.notification



import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.notification.objects.internal.NotificationStatus
import domains.notification.objects.response.{NotificationSummary}
import database.utils.UserIdentitySql

import java.sql.ResultSet

/** 通知表读写辅助对象，集中处理数据库列到通知响应对象的转换。 */
object NotificationTableSupport:

  private def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  /** 从通知查询行读取通知摘要，包含 actor 身份、payload 和读取状态。 */
  def readNotificationSummary(resultSet: ResultSet): NotificationSummary =
    NotificationSummary(
      id = parseColumn("notifications.id", resultSet.getString("id"), domains.notification.objects.NotificationId.parse),
      kind = parseColumn("notifications.kind", resultSet.getString("kind"), NotificationKind.parse),
      actor = readOptionalUsername(resultSet, "actor").map(_ => readUserIdentity(resultSet, "actor")),
      titleKey = resultSet.getString("title_key"),
      bodyKey = resultSet.getString("body_key"),
      payload = decodePayload(resultSet.getString("payload_json")),
      targetPath = resultSet.getString("target_path"),
      targetAnchor = Option(resultSet.getString("target_anchor")),
      isRead = parseColumn("notifications.status", resultSet.getString("status"), NotificationStatus.parse) == NotificationStatus.Read,
      createdAt = resultSet.getTimestamp("created_at").toInstant
    )

  /** 注意：按领域解析函数读取必填列；这里抛异常表示数据库已有非法值，不是可恢复的用户输入错误。 */
  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 将通知类型编码为数据库列值。 */
  def encodeNotificationKindColumn(kind: NotificationKind): String =
    kind match
      case NotificationKind.BlogReply => NotificationPayload.BlogReplyKind

  private def decodePayload(raw: String): NotificationPayload =
    NotificationPayloadJsonCodec.decode(raw)

  private def readOptionalUsername(resultSet: ResultSet, prefix: String): Option[Username] =
    Option(resultSet.getString(s"${prefix}_username")).map(Username.canonical)
