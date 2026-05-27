package domains.problem.http.api




import domains.problem.http.*
import domains.problem.http.codec.ProblemHttpCodecs.given
import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.objects.request.{CreateProblemRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateProblem:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" =>
        context.handlers.executeDecoded[CreateProblemRequest, ProblemCommands.CreateProblemResult](
          request,
          context.plans.createProblem
        )
    }
