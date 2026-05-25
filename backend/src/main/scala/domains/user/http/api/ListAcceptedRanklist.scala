package domains.user.http.api



import domains.user.http.*
import domains.user.http.mapper.UserHttpRequestMappers
import cats.effect.IO
import domains.user.http.UserHttpPlanDefinitions.{listAcceptedRanklist}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListAcceptedRanklist:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" / "ranklist" / "accepted" =>
        context.handlers.execute(request, UserHttpRequestMappers.ranklistRequest(request.uri.query.params), listAcceptedRanklist)
    }
