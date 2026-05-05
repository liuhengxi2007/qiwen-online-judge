package domains.notification.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.notification.application.{NotificationEventHub, NotificationStreamEvent}
import domains.notification.model.NotificationId
import domains.shared.http.AuthenticatedHttpExecutor
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.{Header, HttpRoutes, Response, Status}
import org.http4s.ServerSentEvent
import org.typelevel.ci.CIString

object NotificationRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = NotificationHttpPlanDefinitions.plans(notificationEventHub)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "notifications" =>
        handlers.execute(
          request,
          (),
          plans.listNotifications
        )

      case request @ GET -> Root / "api" / "notifications" / "unread-count" =>
        handlers.execute(
          request,
          (),
          plans.getUnreadCount
        )

      case request @ POST -> Root / "api" / "notifications" / rawNotificationId / "read" =>
        NotificationId.parse(rawNotificationId) match
          case Left(message) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(notificationId) =>
            handlers.execute(
              request,
              notificationId,
              plans.markNotificationRead
            )

      case request @ POST -> Root / "api" / "notifications" / "read-all" =>
        handlers.execute(
          request,
          (),
          plans.markAllNotificationsRead
        )

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
