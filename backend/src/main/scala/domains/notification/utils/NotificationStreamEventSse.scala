package domains.notification.utils

import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.ServerSentEvent

/** 通知流事件的 SSE 渲染工具。 */
object NotificationStreamEventSse:

  private given Encoder[NotificationStreamEvent] = Encoder.instance {
    case NotificationStreamEvent.NotificationsChanged =>
      io.circe.Json.obj()
  }

  /** 将通知领域事件转换为前端识别的 SSE 事件名和 JSON 载荷。 */
  def toServerSentEvent(event: NotificationStreamEvent): ServerSentEvent =
    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some("notifications_changed"))

  /** 渲染单条 SSE 事件，并补充 EventSource 需要的空行分隔符。 */
  def render(event: NotificationStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"
