package domains.usergroup.http.api

import domains.usergroup.http.mapper.UserGroupHttpResponseMappers
import domains.usergroup.http.mapper.UserGroupHttpRequestMappers



import domains.usergroup.http.*
import domains.usergroup.http.codec.UserGroupHttpCodecs.given
import cats.effect.IO
import domains.usergroup.application.UserGroupCommands
import domains.usergroup.objects.request.{UpdateUserGroupRequest}
import domains.usergroup.objects.{UserGroupSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateUserGroup:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "user-groups" / groupSlug =>
        UserGroupHttpRequestMappers.userGroupSlug(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.executeDecoded[
              UpdateUserGroupRequest,
              (UserGroupSlug, UpdateUserGroupRequest),
              UserGroupCommands.UpdateUserGroupResult
            ](
              request,
              UserGroupHttpPlanDefinitions.updateUserGroup
            )(updateRequest => (parsedGroupSlug, updateRequest))
    }
