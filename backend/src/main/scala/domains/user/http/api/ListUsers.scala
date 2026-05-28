package domains.user.http.api

import cats.effect.IO
import domains.auth.http.SiteManagerApi
import domains.auth.objects.SiteManagerUser
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.objects.request.{UserListRequest, UserSearchQuery}
import domains.user.objects.response.UserListResponse
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.utils.PageRequestQuerySupport
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

object ListUsers extends SiteManagerApi[UserListRequest, UserListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserListResponse] = summon[Encoder[UserListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[UserListRequest] =
    val _ = pathParams
    IO.pure(
      UserListRequest(
        query = request.uri.query.params.get("q").flatMap(rawQuery => UserSearchQuery.parse(rawQuery).toOption),
        pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
      )
    )

  override def plan(connection: Connection, actor: SiteManagerUser, request: UserListRequest): IO[UserListResponse] =
    UserTable.listUsers(connection, actor, request)
