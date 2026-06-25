package domains.message.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageEventHubContext, MessageStreamEvent}
import domains.message.table.message.MessageReadTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, PathParams}
import shared.objects.transport.SuccessResponse

import java.sql.Connection

/** 标记当前用户所有私信已读的认证 API，并向对端发布读回执事件。 */
final class MarkAllMessagesRead(messageEventHub: MessageEventHubContext) extends AuthenticatedApi[Unit, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/mark-all-read")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 批量已读操作不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 先收集未读会话读回执，再批量标记已读，最后向对端和自己发布消息流事件。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: Unit
  ): IO[SuccessResponse] =
    val _ = input
    for
      receipts <- MessageReadTable.listUnreadConversationReadReceipts(connection, actor.username)
      _ <- MessageReadTable.markAllMessagesRead(connection, actor.username)
      _ <- receipts.traverse_ { case (conversationId, otherParticipant, readUpToMessageId) =>
        MessageEventHub.publish(
          messageEventHub,
          otherParticipant,
          MessageStreamEvent.ConversationRead(conversationId, readUpToMessageId, actor.username)
        )
      }
      _ <- MessageEventHub.publish(messageEventHub, actor.username, MessageStreamEvent.InboxChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.directMessagesMarkedRead.code),
      message = None,
      params = ApiMessages.directMessagesMarkedRead.params
    )
