package domains.message.objects.request

import io.circe.{Decoder, Encoder}

/** 私信会话已读模式，Message 表示单条消息，Conversation 表示整会话。 */
enum MarkConversationReadMode:
  case Message
  case Conversation

/** 提供已读模式的线格式 codec。 */
object MarkConversationReadMode:
  given Encoder[MarkConversationReadMode] = Encoder.encodeString.contramap(wireValue)
  given Decoder[MarkConversationReadMode] = Decoder.decodeString.emap(parse)

  /** 将内部已读模式映射为 API 线值。 */
  def wireValue(mode: MarkConversationReadMode): String =
    mode match
      case MarkConversationReadMode.Message => "message"
      case MarkConversationReadMode.Conversation => "conversation"

  /** 解析 API 线值，只接受 message 或 conversation。 */
  def parse(raw: String): Either[String, MarkConversationReadMode] =
    raw match
      case "message" => Right(MarkConversationReadMode.Message)
      case "conversation" => Right(MarkConversationReadMode.Conversation)
      case other => Left(s"Unsupported mark conversation read mode: $other")
