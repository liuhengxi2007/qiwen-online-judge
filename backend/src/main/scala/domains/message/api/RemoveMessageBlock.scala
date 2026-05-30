package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.utils.{MessageEventHub, MessageStreamEvent}
import domains.message.table.message.MessageBlockTable
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final class RemoveMessageBlock(messageEventHub: MessageEventHub) extends AuthenticatedApi[Username, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/blocks/:targetUsername/remove")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername").map(Username.canonical))

  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[SuccessResponse] =
    for
      _ <- MessageBlockTable.removeBlock(connection, actor.username, targetUsername)
      _ <- messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.directMessageBlockRemoved.code),
      message = None,
      params = ApiMessages.directMessageBlockRemoved.params
    )
