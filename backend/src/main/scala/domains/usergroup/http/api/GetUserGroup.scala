package domains.usergroup.http.api

import domains.usergroup.http.mapper.UserGroupHttpResponseMappers
import domains.usergroup.http.mapper.UserGroupHttpRequestMappers



import domains.usergroup.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetUserGroup:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "user-groups" / groupSlug =>
        UserGroupHttpRequestMappers.userGroupSlug(groupSlug) match
          case Left(message) =>
            UserGroupHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedGroupSlug) =>
            handlers.execute(request, parsedGroupSlug, UserGroupHttpPlanDefinitions.getUserGroup)
    }
