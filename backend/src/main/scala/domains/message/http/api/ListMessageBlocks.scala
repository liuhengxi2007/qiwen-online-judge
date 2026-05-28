package domains.message.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.message.http.codec.MessageHttpCodecs.given
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.MessageBlockTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

object ListMessageBlocks extends AuthenticatedApi[Unit, List[MessageBlockEntry]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/blocks")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[MessageBlockEntry]] = Encoder.encodeList[MessageBlockEntry]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(connection: Connection, actor: AuthUser, input: Unit): IO[List[MessageBlockEntry]] =
    val _ = input
    MessageBlockTable.listBlocks(connection, actor.username)
