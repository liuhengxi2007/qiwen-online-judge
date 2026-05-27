package domains.message.http.codec

import domains.message.objects.request.*
import domains.message.objects.response.*
import domains.message.http.codec.MessageModelHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs
import domains.user.http.codec.UserModelHttpCodecs.given
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object MessageHttpCodecs:
  export MessageModelHttpCodecs.given
  export UserModelHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Encoder[MarkConversationReadMode] = Encoder.encodeString.contramap {
    case MarkConversationReadMode.Message => "message"
    case MarkConversationReadMode.Conversation => "conversation"
  }

  given Decoder[MarkConversationReadMode] = Decoder.decodeString.emap {
    case "message" => Right(MarkConversationReadMode.Message)
    case "conversation" => Right(MarkConversationReadMode.Conversation)
    case other => Left(s"Unsupported mark conversation read mode: $other")
  }

  given Encoder[CreateConversationRequest] = deriveEncoder[CreateConversationRequest]
  given Decoder[CreateConversationRequest] = deriveDecoder[CreateConversationRequest]
  given Encoder[SendDirectMessageRequest] = deriveEncoder[SendDirectMessageRequest]
  given Decoder[SendDirectMessageRequest] = deriveDecoder[SendDirectMessageRequest]
  given Encoder[MarkConversationReadRequest] = deriveEncoder[MarkConversationReadRequest]

  given Decoder[MarkConversationReadRequest] = Decoder.instance { cursor =>
    for
      mode <- cursor.downField("mode").as[MarkConversationReadMode]
      messageId <- cursor.downField("messageId").as[Option[domains.message.objects.MessageId]]
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

  given Encoder[MessageConversationSummary] = deriveEncoder[MessageConversationSummary]
  given Decoder[MessageConversationSummary] = deriveDecoder[MessageConversationSummary]
  given Encoder[DirectMessage] = deriveEncoder[DirectMessage]
  given Decoder[DirectMessage] = deriveDecoder[DirectMessage]
  given Encoder[MessageBlockEntry] = deriveEncoder[MessageBlockEntry]
  given Decoder[MessageBlockEntry] = deriveDecoder[MessageBlockEntry]
  given Encoder[ConversationMessageFacts] = deriveEncoder[ConversationMessageFacts]
  given Decoder[ConversationMessageFacts] = deriveDecoder[ConversationMessageFacts]
  given Encoder[MessageInboxResponse] = deriveEncoder[MessageInboxResponse]
  given Decoder[MessageInboxResponse] = deriveDecoder[MessageInboxResponse]
  given Encoder[MessageHistoryResponse] = deriveEncoder[MessageHistoryResponse]
  given Decoder[MessageHistoryResponse] = deriveDecoder[MessageHistoryResponse]
