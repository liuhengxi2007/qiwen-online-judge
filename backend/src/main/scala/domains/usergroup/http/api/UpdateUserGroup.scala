package domains.usergroup.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.usergroup.http.UserGroupApiSupport
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.objects.request.UpdateUserGroupRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.rules.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateUserGroup extends AuthenticatedApi[(UserGroupSlug, UpdateUserGroupRequest), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(UserGroupSlug, UpdateUserGroupRequest)] =
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      updateRequest <- request.as[UpdateUserGroupRequest]
    yield (groupSlug, updateRequest)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (UserGroupSlug, UpdateUserGroupRequest)
  ): IO[UserGroupDetail] =
    val (groupSlug, request) = input
    for
      validRequest <- HttpApiError.fromEitherBadRequest(UserGroupApiSupport.validateUpdate(request))
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(UserGroupAccessRules.canEdit(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- UserGroupTable.update(connection, group.id, validRequest)
      updated <- UserGroupApiSupport.refreshBySlug(connection, group.slug, "User group disappeared after update")
    yield UserGroupApiSupport.toUserGroupDetail(updated)
