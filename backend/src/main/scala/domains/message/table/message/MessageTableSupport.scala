package domains.message.table.message



import domains.user.model.Username
import domains.message.model.{ConversationReadReceipt, MessageContent, MessageConversationId, MessageId}
import domains.message.application.output.{DirectMessage, MessageBlockEntry, MessageConversationSummary}
import shared.sql.UserIdentitySql.readUserIdentity

import java.sql.ResultSet

object MessageTableSupport:
  def normalizeConversationPair(left: Username, right: Username): (Username, Username) =
    if left.value <= right.value then (left, right) else (right, left)

  def readConversationSummary(resultSet: ResultSet): MessageConversationSummary =
    MessageConversationSummary(
      id = MessageConversationId(resultSet.getObject("id", classOf[java.util.UUID])),
      otherUser = readUserIdentity(resultSet, "other"),
      lastMessagePreview = Option(resultSet.getString("last_message_preview")),
      lastMessageSenderUsername = Option(resultSet.getString("last_message_sender_username")).map(Username.canonical),
      lastMessageAt = resultSet.getTimestamp("last_message_at").toInstant,
      unreadCount = resultSet.getInt("unread_count")
    )

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

  def readBlockEntry(resultSet: ResultSet): MessageBlockEntry =
    MessageBlockEntry(
      user = readUserIdentity(resultSet, "blocked"),
      createdAt = resultSet.getTimestamp("created_at").toInstant
    )

  def readConversationReadReceipt(resultSet: ResultSet): ConversationReadReceipt =
    ConversationReadReceipt(
      conversationId = MessageConversationId(resultSet.getObject("conversation_id", classOf[java.util.UUID])),
      otherParticipant = Username.canonical(resultSet.getString("other_username")),
      readUpToMessageId = MessageId(resultSet.getObject("read_up_to_message_id", classOf[java.util.UUID]))
    )
