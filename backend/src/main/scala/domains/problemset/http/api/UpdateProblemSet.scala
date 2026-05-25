package domains.problemset.http.api

import domains.problemset.http.mapper.ProblemSetHttpResponseMappers
import domains.problemset.http.mapper.ProblemSetHttpRequestMappers



import domains.problemset.http.*
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problemset.model.request.{UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSetSlug}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateProblemSet:

  def routes(context: ProblemSetHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetHttpRequestMappers.problemSetSlug(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponseMappers.validationErrorResponse(message)
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
