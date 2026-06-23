package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.notification.utils.{NotificationEventHub, NotificationEventHubContext, NotificationStreamEvent}
import domains.user.objects.Username
import fs2.{Stream, text}
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.{Header, Method, Request, Response, ServerSentEvent, Status}
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
          SubscribeNotificationEvents.renderedEventStream(notificationEventHub, actor.username).through(text.utf8.encode)
        )
    )

object SubscribeNotificationEvents:

  private given Encoder[NotificationStreamEvent] = Encoder.instance {
    case NotificationStreamEvent.NotificationsChanged =>
      io.circe.Json.obj()
  }

  /** 生成当前用户通知变更事件的 SSE 文本流，供通知端点和合并实时端点复用。 */
  def renderedEventStream(notificationEventHub: NotificationEventHubContext, username: Username): Stream[IO, String] =
    NotificationEventHub.subscribe(notificationEventHub, username).map(render)

  private def toServerSentEvent(event: NotificationStreamEvent): ServerSentEvent =
    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some("notifications_changed"))

  private def render(event: NotificationStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"
