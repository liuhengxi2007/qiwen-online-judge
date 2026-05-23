package domains.notification.http.api



import domains.notification.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.notification.application.{NotificationEventHub, NotificationStreamEvent}
import domains.notification.model.NotificationId
import shared.http.AuthenticatedHttpExecutor
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.{Header, HttpRoutes, Response, Status}
import org.http4s.ServerSentEvent
import org.typelevel.ci.CIString

object SubscribeNotificationEvents:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = NotificationHttpPlanDefinitions.plans(notificationEventHub)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "notifications" / "events" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
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
        }
    }
  private given Encoder[NotificationStreamEvent] = Encoder.instance {
    case NotificationStreamEvent.NotificationsChanged =>
      io.circe.Json.obj()
  }

  private def toServerSentEvent(event: NotificationStreamEvent): ServerSentEvent =
    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some("notifications_changed"))

  private def toServerSentEventString(event: NotificationStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"

