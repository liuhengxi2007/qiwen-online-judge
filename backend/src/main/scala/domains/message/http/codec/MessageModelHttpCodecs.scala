package domains.message.http.codec

import domains.message.objects.*
import io.circe.{Decoder, Encoder}

object MessageModelHttpCodecs:
  given Encoder[MessageId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[MessageId] = Decoder.decodeString.emap(MessageId.parse)

  given Encoder[MessageConversationId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[MessageConversationId] = Decoder.decodeString.emap(MessageConversationId.parse)

  given Encoder[MessageContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[MessageContent] = Decoder.decodeString.emap(MessageContent.parse)
