package domains.user.http.api



import domains.user.http.*
import cats.effect.IO
import shared.model.PageRequest
import domains.user.http.UserHttpPlanDefinitions.{listUsers}
import domains.user.application.input.{UserListRequest, UserSearchQuery}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListUsers:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" =>
        context.handlers.execute(
          request,
          UserListRequest(
            query = request.uri.query.params.get("q").flatMap(rawQuery => UserSearchQuery.parse(rawQuery).toOption),
            pageRequest = PageRequest(
              page = parsePage(request.uri.query.params.get("page")),
              pageSize = parsePageSize(request.uri.query.params.get("pageSize"))
            )
          ),
          listUsers
        )
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def parsePageSize(rawPageSize: Option[String]): Int =
    rawPageSize.flatMap(_.toIntOption).filter(_ > 0).getOrElse(10)
