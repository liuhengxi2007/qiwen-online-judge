package domains.problemset.http.api

import domains.problemset.http.response.ProblemSetHttpResponses



import domains.problemset.http.*
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problemset.application.input.{UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSetSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateProblemSet:

  def routes(context: ProblemSetHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            context.handlers.executeDecoded[
              UpdateProblemSetRequest,
              (ProblemSetSlug, UpdateProblemSetRequest),
              ProblemSetCommands.UpdateProblemSetResult
            ](
              request,
              ProblemSetHttpPlanDefinitions.updateProblemSet
            )(updateRequest => (parsedProblemSetSlug, updateRequest))
    }