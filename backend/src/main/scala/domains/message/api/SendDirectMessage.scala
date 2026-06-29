package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.objects.MessageConversationId
import domains.message.objects.request.SendDirectMessageRequest
import domains.message.objects.response.DirectMessage
import domains.message.table.message.{DirectMessageTable, MessageBlockTable, MessageConversationTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 发送私信的认证 API，要求当前用户属于会话且未被收件人屏蔽。 */
final class SendDirectMessage(messageEventHub: MessageEventHubContext)
    extends AuthenticatedApi[(MessageConversationId, SendDirectMessageRequest), DirectMessage]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/conversations/:conversationId/messages")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[DirectMessage] = summon[Encoder[DirectMessage]]

  /** 从路径解析会话 id 并读取消息发送请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(MessageConversationId, SendDirectMessageRequest)] =
    for
      conversationId <- HttpApiError.fromEitherBadRequest(pathParams.require("conversationId").flatMap(MessageConversationId.parse))
      body <- request.as[SendDirectMessageRequest]
    yield (conversationId, body)

  /** 查找对端、检查屏蔽关系、写入消息并向收件人发布消息接收事件。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (MessageConversationId, SendDirectMessageRequest)
  ): IO[DirectMessage] =
    val (conversationId, request) = input
    for
      maybeRecipient <- MessageConversationTable.findOtherParticipant(connection, actor.username, conversationId)
      recipientUsername <- maybeRecipient match
        case Some(recipientUsername) => IO.pure(recipientUsername)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.directMessageConversationNotFound))
      blocked <- MessageBlockTable.isBlocked(connection, recipientUsername, actor.username)
      _ <- HttpApiError.ensure(!blocked, HttpApiError.forbidden(ApiMessages.directMessageBlockedByRecipient))
      message <- DirectMessageTable.insertMessage(connection, conversationId, actor.username, recipientUsername, request.content)
      /** 注意：这里只向收件人 SSE 发布新消息；发送方其他标签页依赖本次 HTTP 响应或后续刷新同步 inbox 摘要。 */
      _ <- MessageEventHub.publish(messageEventHub, recipientUsername, MessageStreamEvent.MessageReceived(message))
    yield message
