package domains.usergroup.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.auth.table.auth_user.AuthUserTable
import domains.usergroup.http.UserGroupApiSupport
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.objects.request.AddUserGroupMemberRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.rules.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object AddUserGroupMember extends AuthenticatedApi[(UserGroupSlug, AddUserGroupMemberRequest), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug/members")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(UserGroupSlug, AddUserGroupMemberRequest)] =
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      addRequest <- request.as[AddUserGroupMemberRequest]
    yield (groupSlug, addRequest)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (UserGroupSlug, AddUserGroupMemberRequest)
  ): IO[UserGroupDetail] =
    val (groupSlug, request) = input
    for
      validRequest <- HttpApiError.fromEitherBadRequest(UserGroupApiSupport.validateAddMember(request))
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(UserGroupAccessRules.canEdit(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
      targetUsername <- AuthUserTable.findByUsername(connection, validRequest.username).flatMap {
        case Some(user) => IO.pure(user.username)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
      result <- UserGroupTable.addMember(connection, group.id, validRequest.copy(username = targetUsername))
      _ <- result match
        case UserGroupTable.AddMemberTableResult.AlreadyExists =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.userAlreadyMemberOfGroup))
        case UserGroupTable.AddMemberTableResult.UserNotFound =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
        case UserGroupTable.AddMemberTableResult.Added =>
          IO.unit
      updated <- UserGroupApiSupport.refreshBySlug(connection, group.slug, "User group disappeared after member add")
    yield UserGroupApiSupport.toUserGroupDetail(updated)
