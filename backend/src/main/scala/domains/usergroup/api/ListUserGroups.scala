package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.usergroup.objects.response.UserGroupSummary
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 用户组列表 API，仅返回当前用户可见的用户组摘要分页。 */
object ListUserGroups extends AuthenticatedApi[PageRequest, PageResponse[UserGroupSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/user-groups")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[UserGroupSummary]] = summon[Encoder[PageResponse[UserGroupSummary]]]

  /** 从查询参数解析分页请求。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  /** 按权限返回可见用户组；如果未来禁用列表权限，则返回空分页而不报错。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, pageRequest: PageRequest): IO[PageResponse[UserGroupSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    if !UserGroupAccessRules.canList(actor) then
      IO.pure(PageResponse(items = Nil, page = normalizedPageRequest.page, pageSize = normalizedPageRequest.pageSize, totalItems = 0L))
    else
      UserGroupTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
