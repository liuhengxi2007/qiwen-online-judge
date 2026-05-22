package domains.message.application.input

import domains.message.model.*

import domains.auth.model.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateConversationRequest(
  targetUsername: Username
)

object CreateConversationRequest:
  given Encoder[CreateConversationRequest] = deriveEncoder[CreateConversationRequest]
  given Decoder[CreateConversationRequest] = deriveDecoder[CreateConversationRequest]
