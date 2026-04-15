package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemSlug, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new ProblemHttpHandlers(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        handlers.listProblems(request)

      case request @ POST -> Root / "api" / "problems" =>
        handlers.createProblem(request)

      case request @ GET -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.getProblem(request, parsedProblemSlug)

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.listProblemData(request, parsedProblemSlug)

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" / filename =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                handlers.downloadProblemData(request, parsedProblemSlug, parsedFilename)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / filename / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                handlers.deleteProblemData(request, parsedProblemSlug, parsedFilename)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "clear" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.clearProblemData(request, parsedProblemSlug)

      case request @ POST -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.updateProblem(request, parsedProblemSlug)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.updateProblemData(request, parsedProblemSlug)

      case request @ POST -> Root / "api" / "problems" / problemSlug / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            handlers.deleteProblem(request, parsedProblemSlug)
    }
