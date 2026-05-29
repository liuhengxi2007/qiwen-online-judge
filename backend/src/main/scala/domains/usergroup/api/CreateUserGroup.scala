package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.{AuthenticatedApi, ResolveAccountUsername}
import domains.auth.objects.AuthUser
import domains.user.objects.Username
import domains.usergroup.utils.UserGroupMutationValidation

import domains.usergroup.objects.request.CreateUserGroupRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object CreateUserGroup extends AuthenticatedApi[CreateUserGroupRequest, UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateUserGroupRequest] =
    val _ = pathParams
    request.as[CreateUserGroupRequest]

  override def plan(connection: Connection, actor: AuthUser, request: CreateUserGroupRequest): IO[UserGroupDetail] =
    for
      _ <- HttpApiError.ensure(
        UserGroupAccessRules.canCreate(actor),
        HttpApiError.forbidden(ApiMessages.userGroupCreationForbidden)
      )
      validRequest <- HttpApiError.fromEitherBadRequest(UserGroupMutationValidation.validateCreate(request))
      existing <- UserGroupTable.findBySlug(connection, validRequest.slug)
      _ <- HttpApiError.ensure(existing.isEmpty, HttpApiError.conflict(ApiMessages.userGroupSlugExists))
      conflictingUser <- ResolveAccountUsername.plan(connection, Username.canonical(validRequest.slug.value)).map(_.username.nonEmpty)
      _ <- HttpApiError.ensure(!conflictingUser, HttpApiError.conflict(ApiMessages.userGroupSlugConflictsWithUsername))
      group <- UserGroupTable.insert(connection, actor.username, validRequest)
    yield UserGroupDetail.fromUserGroup(group)
