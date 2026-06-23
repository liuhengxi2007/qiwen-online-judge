package domains.realtime.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageEventHubContext, MessageStreamEvent, MessageStreamEventSse}
import domains.notification.utils.{NotificationEventHub, NotificationEventHubContext, NotificationStreamEvent, NotificationStreamEventSse}
import fs2.text
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 当前用户的合并 SSE 流，用于低频实时 UI 状态失效提示。 */
final class SubscribeRealtimeEvents(
  messageEventHub: MessageEventHubContext,
  notificationEventHub: NotificationEventHubContext
) extends AuthenticatedResponseApi[Unit]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/realtime/events")

  /** SSE 订阅请求不携带路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 完成一次认证后构造流式响应；响应 body 流不持有 JDBC 连接。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: Unit
  ): IO[Response[IO]] =
    val _ = (connection, input)
    val messageEvents = MessageEventHub.subscribe(messageEventHub, actor.username).map(RealtimeStreamEvent.Message(_))
    val notificationEvents = NotificationEventHub.subscribe(notificationEventHub, actor.username).map(RealtimeStreamEvent.Notification(_))

    IO.pure(
      Response[IO](status = Status.Ok)
        .putHeaders(
          Header.Raw(CIString("Content-Type"), "text/event-stream"),
          Header.Raw(CIString("Cache-Control"), "no-cache")
        )
        .withBodyStream(messageEvents.merge(notificationEvents).map(renderRealtimeEvent).through(text.utf8.encode))
    )

  private enum RealtimeStreamEvent:
    case Message(event: MessageStreamEvent)
    case Notification(event: NotificationStreamEvent)

  private def renderRealtimeEvent(event: RealtimeStreamEvent): String =
    event match
      case RealtimeStreamEvent.Message(messageEvent) => MessageStreamEventSse.render(messageEvent)
      case RealtimeStreamEvent.Notification(notificationEvent) => NotificationStreamEventSse.render(notificationEvent)
