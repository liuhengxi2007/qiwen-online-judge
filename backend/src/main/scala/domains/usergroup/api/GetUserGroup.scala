package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser

import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.rules.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetUserGroup extends AuthenticatedApi[UserGroupSlug, UserGroupDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[UserGroupSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))

  override def plan(connection: Connection, actor: AuthUser, groupSlug: UserGroupSlug): IO[UserGroupDetail] =
    UserGroupTable.findBySlug(connection, groupSlug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      case Some(group) =>
        for
          _ <- HttpApiError.ensure(UserGroupAccessRules.canView(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
        yield UserGroupDetail.fromUserGroup(group)
    }
