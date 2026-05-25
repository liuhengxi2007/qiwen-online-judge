package domains.problem.http.api

import domains.problem.http.mapper.ProblemHttpResponseMappers
import domains.problem.http.mapper.ProblemHttpRequestMappers



import domains.problem.http.*
import domains.problem.http.codec.ProblemHttpCodecs.given
import cats.effect.IO
import domains.problem.application.ProblemCommands
import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import domains.problem.model.ProblemSlug
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object SetProblemDataReady:

  def routes(context: ProblemHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "ready" =>
        ProblemHttpRequestMappers.problemSlug(problemSlug) match
          case Left(message) =>
            ProblemHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            context.handlers.executeDecoded[SetProblemReadyRequest, (ProblemSlug, SetProblemReadyRequest), ProblemCommands.SetProblemReadyResult](
              request,
              context.plans.setProblemReady
            ) { readyRequest => (parsedProblemSlug, readyRequest) }
    }
