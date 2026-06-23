package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.notification.utils.{NotificationEventHub, NotificationEventHubContext, NotificationStreamEventSse}
import fs2.text
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 订阅当前用户通知变更的 SSE API，事件由通知事件中心按用户名过滤。 */
final class SubscribeNotificationEvents(notificationEventHub: NotificationEventHubContext) extends AuthenticatedResponseApi[Unit]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/notifications/events")

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
          NotificationEventHub.subscribe(notificationEventHub, actor.username).map(NotificationStreamEventSse.render).through(text.utf8.encode)
        )
    )
