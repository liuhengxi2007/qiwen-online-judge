package domains.message.application.input

import domains.message.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SendDirectMessageRequest(
  content: MessageContent
)

object SendDirectMessageRequest:
  given Encoder[SendDirectMessageRequest] = deriveEncoder[SendDirectMessageRequest]
  given Decoder[SendDirectMessageRequest] = deriveDecoder[SendDirectMessageRequest]
