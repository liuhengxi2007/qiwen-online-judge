package domains.problemset.http.api



import domains.problemset.http.*
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problemset.model.request.{CreateProblemSetRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object CreateProblemSet:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" =>
        handlers.executeDecoded[CreateProblemSetRequest, ProblemSetCommands.CreateProblemSetResult](
          request,
          ProblemSetHttpPlanDefinitions.createProblemSet
        )
    }
