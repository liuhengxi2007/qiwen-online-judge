package domains.message.objects.request

import domains.message.objects.*
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.generic.semiauto.deriveEncoder

final case class MarkConversationReadRequest(
  mode: MarkConversationReadMode,
  messageId: Option[MessageId]
)

object MarkConversationReadRequest:
  given Encoder[MarkConversationReadRequest] = deriveEncoder[MarkConversationReadRequest]

  given Decoder[MarkConversationReadRequest] = Decoder.instance { cursor =>
    for
      mode <- cursor.downField("mode").as[MarkConversationReadMode]
      messageId <- cursor.downField("messageId").as[Option[MessageId]]
      _ <- mode match
        case MarkConversationReadMode.Message if messageId.isEmpty =>
          Left(DecodingFailure("messageId is required when mode is message", cursor.history))
        case _ =>
          Right(())
    yield MarkConversationReadRequest(
      mode = mode,
      messageId = mode match
        case MarkConversationReadMode.Conversation => None
        case MarkConversationReadMode.Message => messageId
    )
  }
