package domains.message.utils



import cats.effect.{IO, Resource}
import domains.user.objects.Username
import domains.message.objects.response.{DirectMessage}
import domains.message.objects.{MessageConversationId, MessageId}
import fs2.Stream
import fs2.concurrent.Topic

sealed trait MessageStreamEvent

object MessageStreamEvent:
  final case class MessageReceived(message: DirectMessage) extends MessageStreamEvent
  final case class ConversationRead(
    conversationId: MessageConversationId,
    readUpToMessageId: MessageId,
    readerUsername: Username
  ) extends MessageStreamEvent
  case object InboxChanged extends MessageStreamEvent

final class MessageEventHub private (topic: Topic[IO, Option[(Username, MessageStreamEvent)]]):

  def publish(username: Username, event: MessageStreamEvent): IO[Unit] =
    topic.publish1(Some((username, event))).void

  def subscribe(username: Username): Stream[IO, MessageStreamEvent] =
    topic.subscribe(128).unNone.filter { case (targetUsername, _) =>
      targetUsername == username
    }.map(_._2)

object MessageEventHub:
  def resource: Resource[IO, MessageEventHub] =
    Resource.eval(Topic[IO, Option[(Username, MessageStreamEvent)]]).map(new MessageEventHub(_))
