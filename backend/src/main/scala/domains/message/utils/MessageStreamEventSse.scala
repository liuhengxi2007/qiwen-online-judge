package domains.message.utils

import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.ServerSentEvent

/** 私信流事件的 SSE 渲染工具。 */
object MessageStreamEventSse:

  private given Encoder[MessageStreamEvent] = Encoder.instance {
    case MessageStreamEvent.MessageReceived(message) =>
      message.asJson
    case MessageStreamEvent.ConversationRead(conversationId, readUpToMessageId, readerUsername) =>
      io.circe.Json.obj(
        "conversationId" -> conversationId.asJson,
        "readUpToMessageId" -> readUpToMessageId.asJson,
        "readerUsername" -> readerUsername.asJson
      )
    case MessageStreamEvent.InboxChanged =>
      io.circe.Json.obj()
  }

  /** 将私信领域事件转换为前端识别的 SSE 事件名和 JSON 载荷。 */
  def toServerSentEvent(event: MessageStreamEvent): ServerSentEvent =
    val eventName = event match
      case _: MessageStreamEvent.MessageReceived => "message_received"
      case _: MessageStreamEvent.ConversationRead => "conversation_read"
      case MessageStreamEvent.InboxChanged => "inbox_changed"

    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some(eventName))

  /** 渲染单条 SSE 事件，并补充 EventSource 需要的空行分隔符。 */
  def render(event: MessageStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"
