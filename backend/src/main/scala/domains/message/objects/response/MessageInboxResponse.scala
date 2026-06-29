package domains.message.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 私信收件箱分页响应，包含会话列表、总未读数和分页元数据。 */
final case class MessageInboxResponse(
  conversations: List[MessageConversationSummary],
  totalUnreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)

/** 提供私信收件箱响应 JSON codec。 */
object MessageInboxResponse:
  given Encoder[MessageInboxResponse] = deriveEncoder[MessageInboxResponse]
  given Decoder[MessageInboxResponse] = deriveDecoder[MessageInboxResponse]
