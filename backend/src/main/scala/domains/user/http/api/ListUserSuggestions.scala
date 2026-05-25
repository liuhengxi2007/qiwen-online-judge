package domains.user.http.api

import domains.user.http.mapper.UserHttpResponseMappers
import domains.user.http.mapper.UserHttpRequestMappers



import domains.user.http.*
import cats.effect.IO
import domains.user.http.UserHttpPlanDefinitions.{listUserSuggestions}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListUserSuggestions:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" / "suggestions" =>
        UserHttpRequestMappers.userSearchQuery(request.uri.query.params) match
          case Left(message) => UserHttpResponseMappers.validationErrorResponse(message)
          case Right(query) => context.handlers.execute(request, query, listUserSuggestions)
    }
