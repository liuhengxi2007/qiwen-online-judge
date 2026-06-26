package routes

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.api.SubscribeMessageEvents
import domains.message.api.MessageEventHubContext
import domains.notification.api.SubscribeNotificationEvents
import domains.notification.api.NotificationEventHubContext
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
    val messageEvents = SubscribeMessageEvents.renderedEventStream(messageEventHub, actor.username)
    val notificationEvents = SubscribeNotificationEvents.renderedEventStream(notificationEventHub, actor.username)

    IO.pure(
      Response[IO](status = Status.Ok)
        .putHeaders(
          Header.Raw(CIString("Content-Type"), "text/event-stream"),
          Header.Raw(CIString("Cache-Control"), "no-cache")
        )
        .withBodyStream(messageEvents.merge(notificationEvents).through(text.utf8.encode))
    )
