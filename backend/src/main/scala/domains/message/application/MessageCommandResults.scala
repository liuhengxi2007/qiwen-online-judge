package domains.message.application



import domains.user.objects.Username
import domains.message.objects.MessageId
import domains.message.objects.internal.ConversationReadReceipt
import domains.message.objects.response.{DirectMessage, MessageBlockEntry, MessageConversationSummary, MessageHistoryResponse}

object MessageCommandResults:

  enum CreateConversationResult:
    case TargetUserNotFound
    case CannotMessageSelf
    case Ready(conversation: MessageConversationSummary)

  enum GetConversationHistoryResult:
    case ConversationNotFound
    case Found(history: MessageHistoryResponse)

  enum SendMessageResult:
    case ConversationNotFound
    case BlockedByRecipient
    case Sent(message: DirectMessage, recipientUsername: Username)

  enum MarkConversationReadResult:
    case ConversationNotFound
    case Marked(summary: MessageConversationSummary, otherParticipant: Username, readUpToMessageId: Option[MessageId])

  enum AddBlockResult:
    case TargetUserNotFound
    case CannotBlockSelf
    case Added(entry: MessageBlockEntry)

  enum RemoveBlockResult:
    case Removed

  final case class MarkAllMessagesReadResult(
    receipts: List[ConversationReadReceipt]
  )
