package domains.usergroup.http.api

import domains.usergroup.http.response.UserGroupHttpResponses



import domains.usergroup.http.*
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import cats.effect.IO
import domains.usergroup.application.UserGroupCommands
import domains.user.model.Username
import domains.usergroup.application.input.{AddUserGroupMemberRequest, CreateUserGroupRequest, UpdateUserGroupMemberRoleRequest, UpdateUserGroupRequest}
import domains.usergroup.model.{UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateUserGroupMemberRole:

  def routes(context: UserGroupHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "role" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            context.handlers.executeDecoded[
              UpdateUserGroupMemberRoleRequest,
              (UserGroupSlug, Username, UpdateUserGroupMemberRoleRequest),
              UserGroupCommands.UpdateUserGroupMemberRoleResult
            ](
              request,
              UserGroupHttpPlanDefinitions.updateMemberRole
            )(updateRoleRequest => (parsedGroupSlug, Username.canonical(memberUsername), updateRoleRequest))
    }