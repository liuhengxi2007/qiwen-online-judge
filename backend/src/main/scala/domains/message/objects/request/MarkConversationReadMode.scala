package domains.message.objects.request

import io.circe.{Decoder, Encoder}

enum MarkConversationReadMode:
  case Message
  case Conversation

object MarkConversationReadMode:
  given Encoder[MarkConversationReadMode] = Encoder.encodeString.contramap(wireValue)
  given Decoder[MarkConversationReadMode] = Decoder.decodeString.emap(parse)

  def wireValue(mode: MarkConversationReadMode): String =
    mode match
      case MarkConversationReadMode.Message => "message"
      case MarkConversationReadMode.Conversation => "conversation"

  def parse(raw: String): Either[String, MarkConversationReadMode] =
    raw match
      case "message" => Right(MarkConversationReadMode.Message)
      case "conversation" => Right(MarkConversationReadMode.Conversation)
      case other => Left(s"Unsupported mark conversation read mode: $other")
