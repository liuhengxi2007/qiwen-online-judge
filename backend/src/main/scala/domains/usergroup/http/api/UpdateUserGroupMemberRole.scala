package domains.usergroup.http.api

import cats.effect.IO
import domains.user.objects.Username
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.usergroup.http.UserGroupApiSupport
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import domains.usergroup.objects.{UserGroupRole, UserGroupSlug}
import domains.usergroup.objects.request.UpdateUserGroupMemberRoleRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.rules.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateUserGroupMemberRole extends AuthenticatedApi[(UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug/members/:memberUsername/role")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  override def decode(
    request: Request[IO],
    pathParams: PathParams
  ): IO[(UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest)] =
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("memberUsername"))
      updateRequest <- request.as[UpdateUserGroupMemberRoleRequest]
    yield (groupSlug, Username.canonical(rawUsername), updateRequest)

  override def plan(
    connection: Connection,
    actor: AuthUser,
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
      updated <- UserGroupApiSupport.refreshBySlug(connection, group.slug, "User group disappeared after member role update")
    yield UserGroupApiSupport.toUserGroupDetail(updated)
