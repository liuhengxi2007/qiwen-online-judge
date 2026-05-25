package domains.problemset.http.api



import domains.problemset.http.*
import domains.problemset.http.mapper.ProblemSetHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblemSets:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" =>
        handlers.execute(request, ProblemSetHttpRequestMappers.listProblemSetsRequest(request.uri.query.params), ProblemSetHttpPlanDefinitions.listProblemSets)
    }
