package domains.notification.application

import cats.effect.{IO, Resource}
import domains.auth.model.Username
import fs2.Stream
import fs2.concurrent.Topic

final class NotificationEventHub private (topic: Topic[IO, Option[(Username, NotificationStreamEvent)]]) :

  def publish(username: Username, event: NotificationStreamEvent): IO[Unit] =
    topic.publish1(Some((username, event))).void

  def subscribe(username: Username): Stream[IO, NotificationStreamEvent] =
    topic.subscribe(128).unNone.filter { case (targetUsername, _) =>
      targetUsername == username
    }.map(_._2)

object NotificationEventHub:
  def resource: Resource[IO, NotificationEventHub] =
    Resource.eval(Topic[IO, Option[(Username, NotificationStreamEvent)]]).map(new NotificationEventHub(_))
