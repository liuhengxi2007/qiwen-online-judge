package domains.usergroup.http.api

import domains.usergroup.http.response.UserGroupHttpResponses



import domains.usergroup.http.*
import cats.effect.IO
import domains.user.model.Username
import domains.usergroup.model.{UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object RemoveUserGroupMember:

  def routes(context: UserGroupHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "user-groups" / groupSlug / "members" / memberUsername / "remove" =>
        UserGroupSlug.parse(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponses.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            context.handlers.execute(
              request,
              (parsedGroupSlug, Username.canonical(memberUsername)),
              UserGroupHttpPlanDefinitions.removeMember
            )
    }