package domains.user.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.user.http.UserApiSupport
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.objects.response.UserRanklistItem
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

object ListContributionRanklist extends AuthenticatedApi[PageRequest, PageResponse[UserRanklistItem]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/ranklist")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[UserRanklistItem]] = summon[Encoder[PageResponse[UserRanklistItem]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequest(page = request.uri.query.params.get("page").flatMap(_.toIntOption).getOrElse(1)))

  override def plan(connection: Connection, actor: AuthUser, pageRequest: PageRequest): IO[PageResponse[UserRanklistItem]] =
    val _ = actor
    UserTable.listContributionRanklist(connection, PageRequest(page = pageRequest.page, pageSize = UserApiSupport.ranklistPageSize))
