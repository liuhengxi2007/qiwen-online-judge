package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemListRequest, ProblemSearchQuery, ProblemSlug, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.shared.model.PageRequest
import domains.shared.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        handlers.execute(
          request,
          ProblemListRequest(
            query = request.uri.query.params.get("q").flatMap(rawQuery => ProblemSearchQuery.parse(rawQuery).toOption),
            pageRequest = PageRequest(
              page = parsePositiveInt(request.uri.query.params.get("page"), 1),
              pageSize = parsePositiveInt(request.uri.query.params.get("pageSize"), 10)
            )
          ),
          ProblemHttpPlanDefinitions.listProblems
        )

      case request @ GET -> Root / "api" / "problems" / "suggestions" =>
        ProblemSearchQuery.parse(request.uri.query.params.get("q").getOrElse("")) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(query) =>
            handlers.execute(request, query, ProblemHttpPlanDefinitions.listProblemSuggestions)

      case request @ POST -> Root / "api" / "problems" =>
        handlers.executeDecoded[CreateProblemRequest, CreateProblemRequest, ProblemCommands.CreateProblemResult](
          request,
          ProblemHttpPlanDefinitions.createProblem
        )(identity)

      case request @ GET -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, ProblemHttpPlanDefinitions.getProblem)

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, ProblemHttpPlanDefinitions.listProblemData)

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" / filename =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                handlers.execute(request, (parsedProblemSlug, parsedFilename), ProblemHttpPlanDefinitions.downloadProblemData)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / filename / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                handlers.execute(request, (parsedProblemSlug, parsedFilename), ProblemHttpPlanDefinitions.deleteProblemData)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "clear" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, ProblemHttpPlanDefinitions.clearProblemData)

      case request @ POST -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.executeDecoded[UpdateProblemRequest, (ProblemSlug, UpdateProblemRequest), ProblemCommands.UpdateProblemResult](
              request,
              ProblemHttpPlanDefinitions.updateProblem
            ) {
              updateRequest => (parsedProblemSlug, updateRequest)
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.executeDecoded[
              UpdateProblemDataRequest,
              (ProblemSlug, UpdateProblemDataRequest),
              ProblemCommands.UpdateProblemDataResult
            ](
              request,
              ProblemHttpPlanDefinitions.updateProblemData
            ) {
              updateDataRequest => (parsedProblemSlug, updateDataRequest)
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.execute(request, parsedProblemSlug, ProblemHttpPlanDefinitions.deleteProblem)
    }

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)
