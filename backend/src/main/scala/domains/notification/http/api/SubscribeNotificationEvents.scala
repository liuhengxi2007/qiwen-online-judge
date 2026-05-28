package domains.notification.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedResponseApi
import domains.auth.objects.AuthUser
import domains.notification.utils.{NotificationEventHub, NotificationStreamEvent}
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.{Header, Method, Request, Response, ServerSentEvent, Status}
import org.typelevel.ci.CIString
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

final class SubscribeNotificationEvents(notificationEventHub: NotificationEventHub) extends AuthenticatedResponseApi[Unit]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/notifications/events")

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(
    connection: Connection,
    actor: AuthUser,
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
          notificationEventHub.subscribe(actor.username).map(toServerSentEventString).through(text.utf8.encode)
        )
    )

  private given Encoder[NotificationStreamEvent] = Encoder.instance {
    case NotificationStreamEvent.NotificationsChanged =>
      io.circe.Json.obj()
  }

  private def toServerSentEvent(event: NotificationStreamEvent): ServerSentEvent =
    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some("notifications_changed"))

  private def toServerSentEventString(event: NotificationStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"
