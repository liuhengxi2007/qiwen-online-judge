package domains.message.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.message.http.codec.MessageHttpCodecs.given
import domains.message.objects.response.MessageInboxResponse
import domains.message.table.message.MessageConversationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.utils.PageRequestQuerySupport
import shared.http.{ApiPath, PathParams}
import shared.objects.PageRequest

import java.sql.Connection

object ListInbox extends AuthenticatedApi[PageRequest, MessageInboxResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/inbox")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[MessageInboxResponse] = summon[Encoder[MessageInboxResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  override def plan(connection: Connection, actor: AuthUser, pageRequest: PageRequest): IO[MessageInboxResponse] =
    MessageConversationTable.listInbox(connection, actor.username, pageRequest.normalized)
