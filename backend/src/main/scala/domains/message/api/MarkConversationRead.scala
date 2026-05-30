package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageStreamEvent}
import domains.message.objects.MessageConversationId
import domains.message.objects.request.{MarkConversationReadMode, MarkConversationReadRequest}
import domains.message.objects.response.MessageConversationSummary
import domains.message.table.message.{MessageConversationTable, MessageReadTable}
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final class MarkConversationRead(messageEventHub: MessageEventHub)
    extends AuthenticatedApi[(MessageConversationId, MarkConversationReadRequest), MessageConversationSummary]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/conversations/:conversationId/mark-read")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageConversationSummary] = summon[Encoder[MessageConversationSummary]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(MessageConversationId, MarkConversationReadRequest)] =
    for
      conversationId <- HttpApiError.fromEitherBadRequest(pathParams.require("conversationId").flatMap(MessageConversationId.parse))
      body <- request.as[MarkConversationReadRequest]
    yield (conversationId, body)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (MessageConversationId, MarkConversationReadRequest)
  ): IO[MessageConversationSummary] =
    val (conversationId, request) = input
    for
      maybeExistingSummary <- MessageConversationTable.findConversationSummaryForUser(connection, actor.username, conversationId)
      existingSummary <- maybeExistingSummary match
        case Some(summary) => IO.pure(summary)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.directMessageConversationNotFound))
      maybeOtherParticipant <- MessageConversationTable.findOtherParticipant(connection, actor.username, conversationId)
      otherParticipant <- maybeOtherParticipant match
        case Some(username) => IO.pure(username)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.directMessageConversationNotFound))
      readUpToMessageId <- request.mode match
        case MarkConversationReadMode.Message =>
          MessageReadTable.markMessageRead(connection, conversationId, actor.username, request.messageId.get).map {
            case true => request.messageId
            case false => None
          }
        case MarkConversationReadMode.Conversation =>
          MessageReadTable.markConversationRead(connection, conversationId, actor.username)
      updatedSummary <- MessageConversationTable.findConversationSummaryForUser(connection, actor.username, conversationId).map(_.getOrElse(existingSummary))
      _ <- readUpToMessageId match
        case Some(messageId) =>
          messageEventHub.publish(otherParticipant, MessageStreamEvent.ConversationRead(conversationId, messageId, actor.username)) *>
            messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
        case None =>
          IO.unit
    yield updatedSummary
