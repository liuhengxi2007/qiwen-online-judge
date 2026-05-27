package domains.usergroup.http.api

import domains.usergroup.http.mapper.UserGroupHttpResponseMappers
import domains.usergroup.http.mapper.UserGroupHttpRequestMappers



import domains.usergroup.http.*
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import cats.effect.IO
import domains.usergroup.application.UserGroupCommands
import domains.user.objects.Username
import domains.usergroup.objects.request.{UpdateUserGroupMemberRoleRequest}
import domains.usergroup.objects.{UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateUserGroupMemberRole:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "role" =>
        UserGroupHttpRequestMappers.userGroupSlug(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.executeDecoded[
              UpdateUserGroupMemberRoleRequest,
              (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest),
              UserGroupCommands.UpdateUserGroupMemberRoleResult
            ](
              request,
              UserGroupHttpPlanDefinitions.updateMemberRole
            )(updateRoleRequest => (parsedGroupSlug, UserGroupHttpRequestMappers.username(memberUsername), updateRoleRequest))
    }
