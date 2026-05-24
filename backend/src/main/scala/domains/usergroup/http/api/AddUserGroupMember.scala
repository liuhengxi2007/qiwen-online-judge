package domains.usergroup.http.api

import domains.usergroup.http.response.UserGroupHttpResponses



import domains.usergroup.http.*
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import cats.effect.IO
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.application.input.{AddUserGroupMemberRequest}
import domains.usergroup.model.{UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object AddUserGroupMember:

  def routes(context: UserGroupHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            context.handlers.executeDecoded[
              AddUserGroupMemberRequest,
              (UserGroupSlug, AddUserGroupMemberRequest),
              UserGroupCommands.AddUserGroupMemberResult
            ](
              request,
              UserGroupHttpPlanDefinitions.addMember
            )(addMemberRequest => (parsedGroupSlug, addMemberRequest))
    }