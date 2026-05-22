package domains.message.application.input

import domains.message.model.*

import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.generic.semiauto.deriveEncoder

enum MarkConversationReadMode:
  case Message
  case Conversation

object MarkConversationReadMode:
  given Encoder[MarkConversationReadMode] = Encoder.encodeString.contramap {
    case MarkConversationReadMode.Message => "message"
    case MarkConversationReadMode.Conversation => "conversation"
  }

  given Decoder[MarkConversationReadMode] = Decoder.decodeString.emap {
    case "message" => Right(MarkConversationReadMode.Message)
    case "conversation" => Right(MarkConversationReadMode.Conversation)
    case other => Left(s"Unsupported mark conversation read mode: $other")
  }

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
