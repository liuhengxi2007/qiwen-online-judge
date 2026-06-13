package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 删除用户组 API，要求站点管理员或用户组 owner 权限。 */
object DeleteUserGroup extends AuthenticatedApi[UserGroupSlug, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析用户组 slug；请求体被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[UserGroupSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))

  /** 查找用户组并校验删除权限，权限不足以 404 隐藏用户组存在性。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, groupSlug: UserGroupSlug): IO[SuccessResponse] =
    for
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(UserGroupAccessRules.canDelete(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- UserGroupTable.delete(connection, group.id)
    yield SuccessResponse(code = Some(ApiMessages.userGroupDeleted.code), message = None, params = ApiMessages.userGroupDeleted.params)
