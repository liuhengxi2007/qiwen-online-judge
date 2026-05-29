package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.user.utils.UserApiRules

import domains.user.objects.response.UserAcceptedRanklistItem
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

object ListAcceptedRanklist extends AuthenticatedApi[PageRequest, PageResponse[UserAcceptedRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/ranklists/accepted-problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[UserAcceptedRanklistItem]] = summon[Encoder[PageResponse[UserAcceptedRanklistItem]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequest(page = request.uri.query.params.get("page").flatMap(_.toIntOption).getOrElse(1)))

  override def plan(connection: Connection, actor: AuthUser, pageRequest: PageRequest): IO[PageResponse[UserAcceptedRanklistItem]] =
    val _ = actor
    UserTable.listAcceptedRanklist(connection, PageRequest(page = pageRequest.page, pageSize = UserApiRules.ranklistPageSize))
