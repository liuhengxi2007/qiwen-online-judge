package domains.message.table.message



import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.message.objects.{MessageContent, MessageConversationId, MessageId}
import domains.message.objects.internal.ConversationReadReceipt
import domains.message.objects.response.{DirectMessage, MessageBlockEntry, MessageConversationSummary}
import database.utils.UserIdentitySql

import java.sql.ResultSet

/** 私信表读写辅助对象，集中处理 ResultSet 到响应对象的转换。 */
object MessageTableSupport:
  private def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  /** 规范化两名参与者的存储顺序，使同一对用户只对应一个会话。 */
  def normalizeConversationPair(left: Username, right: Username): (Username, Username) =
    if left.value <= right.value then (left, right) else (right, left)

  /** 从会话查询行读取当前用户视角的会话摘要。 */
  def readConversationSummary(resultSet: ResultSet): MessageConversationSummary =
    MessageConversationSummary(
      id = MessageConversationId(resultSet.getObject("id", classOf[java.util.UUID])),
      otherUser = readUserIdentity(resultSet, "other"),
      lastMessagePreview = Option(resultSet.getString("last_message_preview")),
      lastMessageSenderUsername = Option(resultSet.getString("last_message_sender_username")).map(Username.canonical),
      lastMessageAt = resultSet.getTimestamp("last_message_at").toInstant,
      unreadCount = resultSet.getInt("unread_count")
    )

  /** 从消息查询行读取私信响应对象。 */
  def readDirectMessage(resultSet: ResultSet): DirectMessage =
    DirectMessage(
      id = MessageId(resultSet.getObject("id", classOf[java.util.UUID])),
      conversationId = MessageConversationId(resultSet.getObject("conversation_id", classOf[java.util.UUID])),
      sender = readUserIdentity(resultSet, "sender"),
      recipientUsername = Username.canonical(resultSet.getString("recipient_username")),
      content = MessageContent(resultSet.getString("content")),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      readAt = Option(resultSet.getTimestamp("read_at")).map(_.toInstant)
    )

  /** 从屏蔽查询行读取屏蔽名单条目。 */
  def readBlockEntry(resultSet: ResultSet): MessageBlockEntry =
    MessageBlockEntry(
      user = readUserIdentity(resultSet, "blocked"),
      createdAt = resultSet.getTimestamp("created_at").toInstant
    )

  /** 从读回执查询行读取内部读回执对象。 */
  def readConversationReadReceipt(resultSet: ResultSet): ConversationReadReceipt =
    ConversationReadReceipt(
      conversationId = MessageConversationId(resultSet.getObject("conversation_id", classOf[java.util.UUID])),
      otherParticipant = Username.canonical(resultSet.getString("other_username")),
      readUpToMessageId = MessageId(resultSet.getObject("read_up_to_message_id", classOf[java.util.UUID]))
    )
