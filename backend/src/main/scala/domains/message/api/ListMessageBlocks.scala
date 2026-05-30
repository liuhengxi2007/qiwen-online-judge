package domains.message.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.message.objects.response.MessageBlockEntry
import domains.message.table.message.MessageBlockTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

object ListMessageBlocks extends AuthenticatedApi[Unit, List[MessageBlockEntry]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/messages/blocks")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[MessageBlockEntry]] = Encoder.encodeList[MessageBlockEntry]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(connection: Connection, actor: AuthenticatedUser, input: Unit): IO[List[MessageBlockEntry]] =
    val _ = input
    MessageBlockTable.listBlocks(connection, actor.username)
