package domains.hack.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.response.HackListResponse
import domains.hack.table.hack.HackQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

final case class HackListQuery(page: Int, pageSize: Int)

object ListHacks extends AuthenticatedApi[HackListQuery, HackListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/hacks")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[HackListResponse] = summon[Encoder[HackListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[HackListQuery] =
    val _ = pathParams
    val params = request.uri.query.params
    IO.pure(
      HackListQuery(
        page = params.get("page").flatMap(_.toIntOption).getOrElse(1),
        pageSize = params.get("pageSize").flatMap(_.toIntOption).getOrElse(20)
      )
    )

  override def plan(connection: Connection, actor: AuthenticatedUser, query: HackListQuery): IO[HackListResponse] =
    HackQueryTable.listVisible(connection, actor, query.page, query.pageSize)
