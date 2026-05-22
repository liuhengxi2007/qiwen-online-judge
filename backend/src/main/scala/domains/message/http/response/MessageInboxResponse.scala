package domains.message.http.response

import domains.message.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class MessageInboxResponse(
  conversations: List[MessageConversationSummary],
  totalUnreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)

object MessageInboxResponse:
  given Encoder[MessageInboxResponse] = deriveEncoder[MessageInboxResponse]
  given Decoder[MessageInboxResponse] = deriveDecoder[MessageInboxResponse]
