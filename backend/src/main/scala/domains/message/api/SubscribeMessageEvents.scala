package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageEventHubContext, MessageStreamEvent}
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.{Header, Method, Request, Response, ServerSentEvent, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 订阅当前用户私信事件的 SSE API，事件由消息事件中心按用户名过滤。 */
final class SubscribeMessageEvents(messageEventHub: MessageEventHubContext) extends AuthenticatedResponseApi[Unit]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/events")

  /** SSE 订阅不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 构造 text/event-stream 响应，副作用是保持订阅流直到客户端断开。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: Unit
  ): IO[Response[IO]] =
    val _ = (connection, input)
    IO.pure(
      Response[IO](status = Status.Ok)
        .putHeaders(
          Header.Raw(CIString("Content-Type"), "text/event-stream"),
          Header.Raw(CIString("Cache-Control"), "no-cache")
        )
        .withBodyStream(
          MessageEventHub.subscribe(messageEventHub, actor.username).map(toServerSentEventString).through(text.utf8.encode)
        )
    )

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

  /** 将消息流内部事件映射为前端识别的 SSE 事件名和 JSON 数据。 */
  private def toServerSentEvent(event: MessageStreamEvent): ServerSentEvent =
    val eventName = event match
      case _: MessageStreamEvent.MessageReceived => "message_received"
      case _: MessageStreamEvent.ConversationRead => "conversation_read"
      case MessageStreamEvent.InboxChanged => "inbox_changed"

    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some(eventName))

  /** 渲染单条 SSE 事件并补充事件分隔换行。 */
  private def toServerSentEventString(event: MessageStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"
