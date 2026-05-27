package domains.problemset.http.api

import domains.problemset.http.mapper.ProblemSetHttpResponseMappers
import domains.problemset.http.mapper.ProblemSetHttpRequestMappers



import domains.problemset.http.*
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import cats.effect.IO
import domains.problemset.application.ProblemSetCommands
import domains.problemset.objects.request.{AddProblemToProblemSetRequest}
import domains.problemset.objects.ProblemSetSlug
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object AddProblemToProblemSet:

  def routes(handlers: domains.auth.http.AuthenticatedHttpExecutor)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" =>
        ProblemSetHttpRequestMappers.problemSetSlug(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponseMappers.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.executeDecoded[
              AddProblemToProblemSetRequest,
              (ProblemSetSlug, AddProblemToProblemSetRequest),
              ProblemSetCommands.AddProblemResult
            ](
              request,
              ProblemSetHttpPlanDefinitions.addProblem
            )(addRequest => (parsedProblemSetSlug, addRequest))
    }
