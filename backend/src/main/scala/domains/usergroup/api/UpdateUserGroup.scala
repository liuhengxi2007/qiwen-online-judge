package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.usergroup.utils.UserGroupMutationValidation

import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.objects.request.UpdateUserGroupRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 更新用户组基础资料 API，要求站点管理员或组内管理权限。 */
object UpdateUserGroup extends AuthenticatedApi[(UserGroupSlug, UpdateUserGroupRequest), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  /** 从路径解析用户组 slug，并从 JSON 请求体解码更新内容。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(UserGroupSlug, UpdateUserGroupRequest)] =
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      updateRequest <- request.as[UpdateUserGroupRequest]
    yield (groupSlug, updateRequest)

  /** 校验输入和管理权限后更新名称/描述，权限不足以 404 隐藏用户组存在性。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (UserGroupSlug, UpdateUserGroupRequest)
  ): IO[UserGroupDetail] =
    val (groupSlug, request) = input
    for
      validRequest <- HttpApiError.fromEitherBadRequest(UserGroupMutationValidation.validateUpdate(request))
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(UserGroupAccessRules.canEdit(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- UserGroupTable.update(connection, group.id, validRequest)
      updated <- UserGroupMutationValidation.refreshBySlug(connection, group.slug, "User group disappeared after update")
    yield UserGroupDetail.fromUserGroup(updated)
