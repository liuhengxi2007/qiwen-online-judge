package domains.message.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.message.application.{MessageEventHub, MessageStreamEvent}
import domains.message.http.codec.MessageHttpCodecs.given
import domains.message.objects.MessageConversationId
import domains.message.objects.request.SendDirectMessageRequest
import domains.message.objects.response.DirectMessage
import domains.message.table.message.{DirectMessageTable, MessageBlockTable, MessageConversationTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final class SendDirectMessage(messageEventHub: MessageEventHub)
    extends AuthenticatedApi[(MessageConversationId, SendDirectMessageRequest), DirectMessage]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/conversations/:conversationId/messages")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[DirectMessage] = summon[Encoder[DirectMessage]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(MessageConversationId, SendDirectMessageRequest)] =
    for
      conversationId <- HttpApiError.fromEitherBadRequest(pathParams.require("conversationId").flatMap(MessageConversationId.parse))
      body <- request.as[SendDirectMessageRequest]
    yield (conversationId, body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
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
      _ <- messageEventHub.publish(recipientUsername, MessageStreamEvent.MessageReceived(message))
    yield message
