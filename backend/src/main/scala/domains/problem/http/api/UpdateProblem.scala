package domains.problem.http.api

import domains.problem.http.response.ProblemHttpResponses



import domains.problem.http.*
import domains.problem.http.codec.ProblemHttpCodecs.given
import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.application.input.{UpdateProblemRequest}
import domains.problem.model.{ProblemSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateProblem:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            context.handlers.executeDecoded[UpdateProblemRequest, (ProblemSlug, UpdateProblemRequest), ProblemCommands.UpdateProblemResult](
              request,
              context.plans.updateProblem
            ) {
              updateRequest => (parsedProblemSlug, updateRequest)
            }
    }
