package domains.message.objects.request

import domains.message.objects.*
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.generic.semiauto.deriveEncoder

/** 标记会话已读请求体，message 模式必须携带 messageId。 */
final case class MarkConversationReadRequest(
  mode: MarkConversationReadMode,
  messageId: Option[MessageId]
)

/** 提供已读请求体 codec，并在解码阶段校验 mode/messageId 的组合。 */
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
