package domains.notification.utils



import cats.effect.{IO, Resource}
import domains.user.objects.Username
import fs2.Stream
import fs2.concurrent.Topic

/** 基于内存 Topic 的通知事件中心，按用户名过滤事件并提供 SSE 订阅流。 */
final class NotificationEventHub private (topic: Topic[IO, Option[(Username, NotificationStreamEvent)]]) :

  /** 向指定用户发布通知变更事件；事件不持久化，客户端需通过列表/未读数接口补齐状态。 */
  def publish(username: Username, event: NotificationStreamEvent): IO[Unit] =
    topic.publish1(Some((username, event))).void

  /** 订阅指定用户的通知事件流，缓冲区满时遵循 fs2 Topic 的背压语义。 */
  def subscribe(username: Username): Stream[IO, NotificationStreamEvent] =
    topic.subscribe(128).unNone.filter { case (targetUsername, _) =>
      targetUsername == username
    }.map(_._2)

/** 创建 NotificationEventHub 资源的工厂对象。 */
object NotificationEventHub:
  /** 初始化内存 Topic，并在资源生命周期内共享同一个通知事件中心。 */
  def resource: Resource[IO, NotificationEventHub] =
    Resource.eval(Topic[IO, Option[(Username, NotificationStreamEvent)]]).map(new NotificationEventHub(_))
