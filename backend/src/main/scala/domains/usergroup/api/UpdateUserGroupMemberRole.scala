package domains.usergroup.api

import cats.effect.IO
import domains.user.objects.Username
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.usergroup.utils.UserGroupMutationValidation

import domains.usergroup.objects.{UserGroupRole, UserGroupSlug}
import domains.usergroup.objects.request.UpdateUserGroupMemberRoleRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 更新用户组成员角色 API，包含 owner 转移逻辑。 */
object UpdateUserGroupMemberRole extends AuthenticatedApi[(UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug/members/:memberUsername/role")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  /** 从路径解析用户组和成员用户名，并从 JSON 请求体解码目标角色。 */
  override def decode(
    request: Request[IO],
    pathParams: PathParams
  ): IO[(UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest)] =
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("memberUsername"))
      updateRequest <- request.as[UpdateUserGroupMemberRoleRequest]
    yield (groupSlug, Username.canonical(rawUsername), updateRequest)

  /** 要求 owner/site manager 级别权限；目标角色为 owner 时执行所有权转移。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest)
  ): IO[UserGroupDetail] =
    val (groupSlug, targetUsername, request) = input
    for
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(UserGroupAccessRules.canDelete(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
      targetMember <- group.members.find(_.username.value == targetUsername.value) match
        case Some(member) => IO.pure(member)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.groupMemberNotFound))
      _ <- HttpApiError.ensure(
        targetMember.role != UserGroupRole.Owner || request.role == UserGroupRole.Owner,
        HttpApiError.badRequest(ApiMessages.userGroupOwnerModifyForbidden)
      )
      _ <- if request.role == UserGroupRole.Owner then
        UserGroupTable
          .transferOwnership(connection, group.id, group.ownerUsername, targetMember.username)
          .flatMap {
            case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
              HttpApiError.raise(HttpApiError.notFound(ApiMessages.groupMemberNotFound))
            case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
              IO.unit
          }
      else
        UserGroupTable
          .updateMemberRole(connection, group.id, targetMember.username, request.role)
          .flatMap {
            case UserGroupTable.UpdateMemberRoleTableResult.MemberNotFound =>
              HttpApiError.raise(HttpApiError.notFound(ApiMessages.groupMemberNotFound))
            case UserGroupTable.UpdateMemberRoleTableResult.Updated =>
              IO.unit
          }
      updated <- UserGroupMutationValidation.refreshBySlug(connection, group.slug, "User group disappeared after member role update")
    yield UserGroupDetail.fromUserGroup(updated)
