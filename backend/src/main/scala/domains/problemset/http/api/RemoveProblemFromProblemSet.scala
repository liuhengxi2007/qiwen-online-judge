package domains.problemset.http.api

import domains.problemset.http.response.ProblemSetHttpResponses



import domains.problemset.http.*
import cats.effect.IO
import domains.problem.model.ProblemSlug
import domains.problemset.model.{ProblemSetSlug}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object RemoveProblemFromProblemSet:

  def routes(context: ProblemSetHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" / linkedProblemSlug / "remove" =>
        (ProblemSetSlug.parse(problemSetSlug), ProblemSlug.parse(linkedProblemSlug)) match
          case (Left(message), _) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (_, Left(message)) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (Right(parsedProblemSetSlug), Right(parsedProblemSlug)) =>
            context.handlers.execute(request, (parsedProblemSetSlug, parsedProblemSlug), ProblemSetHttpPlanDefinitions.removeProblem)
    }