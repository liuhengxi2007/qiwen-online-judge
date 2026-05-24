package domains.user.http.api

import domains.user.http.response.UserHttpResponses



import domains.user.http.*
import cats.effect.IO
import domains.user.http.UserHttpPlanDefinitions.{listUserSuggestions}
import domains.user.application.input.{UserSearchQuery}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListUserSuggestions:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" / "suggestions" =>
        UserSearchQuery.parse(request.uri.query.params.get("q").getOrElse("")) match
          case Left(message) => UserHttpResponses.validationErrorResponse(message)
          case Right(query) => context.handlers.execute(request, query, listUserSuggestions)
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def parsePageSize(rawPageSize: Option[String]): Int =
    rawPageSize.flatMap(_.toIntOption).filter(_ > 0).getOrElse(10)
