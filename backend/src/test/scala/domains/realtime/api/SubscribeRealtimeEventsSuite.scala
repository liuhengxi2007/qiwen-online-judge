package domains.realtime.api

import cats.effect.IO
import cats.effect.kernel.Outcome
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageStreamEvent}
import domains.notification.utils.{NotificationEventHub, NotificationStreamEvent}
import domains.user.objects.Username
import fs2.text
import munit.CatsEffectSuite
import org.http4s.Status
import org.typelevel.ci.CIString
import shared.api.ApiPath

import scala.concurrent.duration.*

class SubscribeRealtimeEventsSuite extends CatsEffectSuite:

  private val actor = AuthenticatedUser(
    username = Username.canonical("alice"),
    siteManager = false,
    problemManager = false,
    contestManager = false
  )

  test("combined realtime API exposes the new route surface and event-stream headers") {
    MessageEventHub.resource.use { messageHub =>
      NotificationEventHub.resource.use { notificationHub =>
        val api = SubscribeRealtimeEvents(messageHub, notificationHub)
        api.plan(null, actor, ()).map { response =>
          assertEquals(api.path, ApiPath("/api/realtime/events"))
          assertEquals(response.status, Status.Ok)
          assert(response.headers.headers.exists(header => header.name == CIString("Content-Type") && header.value == "text/event-stream"))
        }
      }
    }
  }

  test("combined realtime stream renders message and notification event names") {
    MessageEventHub.resource.use { messageHub =>
      NotificationEventHub.resource.use { notificationHub =>
        val api = SubscribeRealtimeEvents(messageHub, notificationHub)
        for
          response <- api.plan(null, actor, ())
          bodyFiber <- response.body
            .through(text.utf8.decode)
            .scan("")(_ + _)
            .filter(body => body.contains("inbox_changed") && body.contains("notifications_changed"))
            .head
            .compile
            .lastOrError
            .start
          publisherFiber <- (
            MessageEventHub.publish(messageHub, actor.username, MessageStreamEvent.InboxChanged) *>
              NotificationEventHub.publish(notificationHub, actor.username, NotificationStreamEvent.NotificationsChanged) *>
              IO.sleep(50.millis)
          ).foreverM.start
          body <- bodyFiber.join.flatMap {
            case Outcome.Succeeded(result) => result
            case Outcome.Errored(error) => IO.raiseError(error)
            case Outcome.Canceled() => IO.raiseError(new RuntimeException("Realtime stream read was canceled."))
          }.timeout(2.seconds).guarantee(publisherFiber.cancel)
        yield
          assert(body.contains("inbox_changed"))
          assert(body.contains("notifications_changed"))
      }
    }
  }
