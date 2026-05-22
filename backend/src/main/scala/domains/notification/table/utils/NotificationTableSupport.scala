package domains.notification.table.utils



import domains.user.model.Username
import domains.auth.table.utils.UserIdentityTableSupport.readUserIdentity
import domains.notification.model.{NotificationKind, NotificationPayload, NotificationStatus}
import domains.notification.application.output.{NotificationSummary}

import java.sql.ResultSet

object NotificationTableSupport:

  def readNotificationSummary(resultSet: ResultSet): NotificationSummary =
    NotificationSummary(
      id = parseColumn("notifications.id", resultSet.getString("id"), domains.notification.model.NotificationId.parse),
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

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  private def decodePayload(raw: String): NotificationPayload =
    NotificationPayloadJsonCodec.decode(raw)

  private def readOptionalUsername(resultSet: ResultSet, prefix: String): Option[Username] =
    Option(resultSet.getString(s"${prefix}_username")).map(Username.canonical)
