package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.usergroup.utils.UserGroupMutationValidation

import domains.usergroup.objects.{UserGroupRole, UserGroupSlug}
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 移除用户组成员 API，按角色限制 owner/manager 可移除的目标。 */
object RemoveUserGroupMember extends AuthenticatedApi[(UserGroupSlug, Username), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug/members/:memberUsername/unlink")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  /** 从路径解析用户组 slug 和目标成员用户名；请求体被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(UserGroupSlug, Username)] =
    val _ = request
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("memberUsername"))
    yield (groupSlug, Username.canonical(rawUsername))

  /** 校验目标成员存在和移除权限，禁止直接移除 owner，成功后返回刷新详情。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: (UserGroupSlug, Username)): IO[UserGroupDetail] =
    val (groupSlug, targetUsername) = input
    for
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(
        UserGroupAccessRules.canEdit(actor, group),
        HttpApiError.notFound(ApiMessages.userGroupNotFound)
      )
      targetMember <- group.members.find(_.username.value == targetUsername.value) match
        case Some(member) => IO.pure(member)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.groupMemberNotFound))
      _ <- HttpApiError.ensure(targetMember.role != UserGroupRole.Owner, HttpApiError.badRequest(ApiMessages.userGroupOwnerRemoveForbidden))
      _ <- HttpApiError.ensure(
        UserGroupAccessRules.canRemoveMember(actor, group, targetMember.username, targetMember.role),
        HttpApiError.notFound(ApiMessages.userGroupNotFound)
      )
      _ <- UserGroupTable.removeMember(connection, group.id, targetMember.username).flatMap {
        case UserGroupTable.RemoveMemberTableResult.MemberNotFound =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.groupMemberNotFound))
        case UserGroupTable.RemoveMemberTableResult.Removed =>
          IO.unit
      }
      updated <- UserGroupMutationValidation.refreshBySlug(connection, group.slug, "User group disappeared after member removal")
    yield UserGroupDetail.fromUserGroup(updated)
