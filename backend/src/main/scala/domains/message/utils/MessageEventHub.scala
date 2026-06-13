package domains.message.utils



import cats.effect.{IO, Resource}
import domains.user.objects.Username
import domains.message.objects.response.{DirectMessage}
import domains.message.objects.{MessageConversationId, MessageId}
import fs2.Stream
import fs2.concurrent.Topic

/** 私信 SSE 内部事件基类，事件按目标用户名发布给订阅者。 */
sealed trait MessageStreamEvent

/** 私信 SSE 事件集合，覆盖新消息、读回执和收件箱刷新。 */
object MessageStreamEvent:
  final case class MessageReceived(message: DirectMessage) extends MessageStreamEvent
  final case class ConversationRead(
    conversationId: MessageConversationId,
    readUpToMessageId: MessageId,
    readerUsername: Username
  ) extends MessageStreamEvent
  case object InboxChanged extends MessageStreamEvent

/** 基于内存 Topic 的私信事件中心上下文。 */
final case class MessageEventHubContext(topic: Topic[IO, Option[(Username, MessageStreamEvent)]])

/** 基于内存 Topic 的私信事件中心函数集合，按用户名过滤事件并提供 SSE 订阅流。 */
object MessageEventHub:

  /** 向指定用户发布私信事件；事件不会持久化，断线期间需由客户端重新拉取状态补齐。 */
  def publish(context: MessageEventHubContext, username: Username, event: MessageStreamEvent): IO[Unit] =
    context.topic.publish1(Some((username, event))).void

  /** 订阅指定用户的私信事件流，缓冲区满时遵循 fs2 Topic 的背压语义。 */
  def subscribe(context: MessageEventHubContext, username: Username): Stream[IO, MessageStreamEvent] =
    context.topic.subscribe(128).unNone.filter { case (targetUsername, _) =>
      targetUsername == username
    }.map(_._2)

  /** 初始化内存 Topic，并在资源生命周期内共享同一个事件中心。 */
  def resource: Resource[IO, MessageEventHubContext] =
    Resource.eval(Topic[IO, Option[(Username, MessageStreamEvent)]]).map(MessageEventHubContext(_))
