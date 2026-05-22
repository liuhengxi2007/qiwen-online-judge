package domains.problemset.http.api



import domains.problemset.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.problemset.application.ProblemSetCommands
import domains.problem.model.ProblemSlug
import domains.problemset.http.request.{AddProblemToProblemSetRequest, CreateProblemSetRequest, UpdateProblemSetRequest}
import domains.problemset.model.{ProblemSetSlug}
import domains.shared.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object AddProblemToProblemSet:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
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

  private def parsePageRequest(queryParams: Map[String, String]): domains.shared.model.PageRequest =
    domains.shared.model.PageRequest(
      page = parsePositiveInt(queryParams.get("page"), 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)

