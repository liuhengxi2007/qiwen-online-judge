package domains.user.api

import cats.effect.IO
import domains.auth.api.SiteManagerApi
import domains.auth.objects.SiteManagerUser

import domains.user.objects.request.{UserListRequest, UserSearchQuery}
import domains.user.objects.response.UserListResponse
import domains.user.table.user_profile.UserProfileQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 站点管理员用户列表 API，支持搜索和分页。 */
object ListUsers extends SiteManagerApi[UserListRequest, UserListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserListResponse] = summon[Encoder[UserListResponse]]

  /** 从查询参数解析搜索词和分页；非法搜索词会被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[UserListRequest] =
    val _ = pathParams
    IO.pure(
      UserListRequest(
        /** FIXME-CN: q 参数解析失败会被静默当作无搜索条件，可能让非法搜索输入退化为全量列表。 */
        query = request.uri.query.params.get("q").flatMap(rawQuery => UserSearchQuery.parse(rawQuery).toOption),
        pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
      )
    )

  /** 以站点管理员身份查询管理端用户列表。 */
  override def plan(connection: Connection, actor: SiteManagerUser, request: UserListRequest): IO[UserListResponse] =
    UserProfileQueryTable.listUsers(connection, actor, request)
