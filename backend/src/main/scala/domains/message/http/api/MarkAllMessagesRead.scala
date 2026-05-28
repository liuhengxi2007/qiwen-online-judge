package domains.message.http.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.message.application.{MessageEventHub, MessageStreamEvent}
import domains.message.table.message.MessageReadTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final class MarkAllMessagesRead(messageEventHub: MessageEventHub) extends AuthenticatedApi[Unit, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/read-all")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: Unit
  ): IO[SuccessResponse] =
    val _ = input
    for
      receipts <- MessageReadTable.listUnreadConversationReadReceipts(connection, actor.username)
      _ <- MessageReadTable.markAllMessagesRead(connection, actor.username)
      _ <- receipts.traverse_(receipt =>
        messageEventHub.publish(
          receipt.otherParticipant,
          MessageStreamEvent.ConversationRead(receipt.conversationId, receipt.readUpToMessageId, actor.username)
        )
      )
      _ <- messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.directMessagesMarkedRead.code),
      message = None,
      params = ApiMessages.directMessagesMarkedRead.params
    )
