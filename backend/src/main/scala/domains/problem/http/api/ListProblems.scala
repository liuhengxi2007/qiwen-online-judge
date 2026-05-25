package domains.problem.http.api

import domains.problem.http.*
import domains.problem.http.mapper.ProblemHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblems:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        context.handlers.execute(
          request,
          ProblemHttpRequestMappers.listProblemsRequest(request.uri.query.params),
          context.plans.listProblems
        )
    }
