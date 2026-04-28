package domains.message.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class MarkConversationReadRequest()

object MarkConversationReadRequest:
  given Encoder[MarkConversationReadRequest] = deriveEncoder[MarkConversationReadRequest]
  given Decoder[MarkConversationReadRequest] = deriveDecoder[MarkConversationReadRequest]
