package domains.message.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ConversationMessageFacts(
  viewerHasSentMessage: Boolean,
  otherParticipantMessageCount: Int
)

object ConversationMessageFacts:
  given Encoder[ConversationMessageFacts] = deriveEncoder[ConversationMessageFacts]
  given Decoder[ConversationMessageFacts] = deriveDecoder[ConversationMessageFacts]
