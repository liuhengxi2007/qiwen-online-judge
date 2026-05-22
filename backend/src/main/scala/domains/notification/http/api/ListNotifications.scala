package domains.notification.http.api



import domains.notification.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.notification.application.{NotificationEventHub, NotificationStreamEvent}
import domains.notification.model.NotificationId
import domains.shared.http.AuthenticatedHttpExecutor
import domains.shared.model.PageRequest
import fs2.text
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.{Header, HttpRoutes, Response, Status}
import org.http4s.ServerSentEvent
import org.typelevel.ci.CIString

object ListNotifications:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = NotificationHttpPlanDefinitions.plans(notificationEventHub)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "notifications" =>
        handlers.execute(
          request,
          parsePageRequest(request.uri.query.params),
          plans.listNotifications
        )
    }

  private def parsePageRequest(queryParams: Map[String, String]): PageRequest =
    PageRequest(
      page = parsePositiveInt(queryParams.get("page"), 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)

  private given Encoder[NotificationStreamEvent] = Encoder.instance {
    case NotificationStreamEvent.NotificationsChanged =>
      io.circe.Json.obj()
  }

  private def toServerSentEvent(event: NotificationStreamEvent): ServerSentEvent =
    ServerSentEvent(data = Some(event.asJson.noSpaces), eventType = Some("notifications_changed"))

  private def toServerSentEventString(event: NotificationStreamEvent): String =
    toServerSentEvent(event).renderString + "\n"

