package domains.message.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.message.utils.{MessageEventHub, MessageStreamEvent}
import domains.message.http.codec.MessageHttpCodecs.given
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.{MessageBlockTable, MessageUserTable}
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final class AddMessageBlock(messageEventHub: MessageEventHub) extends AuthenticatedApi[Username, MessageBlockEntry]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/messages/blocks/:targetUsername")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageBlockEntry] = summon[Encoder[MessageBlockEntry]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername").map(Username.canonical))

  override def plan(connection: Connection, actor: AuthUser, targetUsername: Username): IO[MessageBlockEntry] =
    for
      _ <- HttpApiError.ensure(actor.username != targetUsername, HttpApiError.badRequest(ApiMessages.directMessageBlockSelfForbidden))
      targetExists <- MessageUserTable.userExists(connection, targetUsername)
      _ <- HttpApiError.ensure(targetExists, HttpApiError.notFound(ApiMessages.userNotFound))
      entry <- MessageBlockTable.upsertBlock(connection, actor.username, targetUsername)
      _ <- messageEventHub.publish(actor.username, MessageStreamEvent.InboxChanged)
    yield entry
