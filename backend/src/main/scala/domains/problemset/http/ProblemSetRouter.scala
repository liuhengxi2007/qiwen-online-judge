package domains.problemset.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problemset.application.ProblemSetCommands
import domains.problem.model.ProblemSlug
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetSlug, UpdateProblemSetRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new ProblemSetHttpHandlers(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" =>
        handlers.listProblemSets(request)

      case request @ GET -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.getProblemSet(request, parsedProblemSetSlug)

      case request @ POST -> Root / "api" / "problem-sets" =>
        handlers.createProblemSet(request)

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.addProblem(request, parsedProblemSetSlug)

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.updateProblemSet(request, parsedProblemSetSlug)

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "delete" =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.deleteProblemSet(request, parsedProblemSetSlug)

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" / linkedProblemSlug / "remove" =>
        (ProblemSetSlug.parse(problemSetSlug), ProblemSlug.parse(linkedProblemSlug)) match
          case (Left(message), _) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (_, Left(message)) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (Right(parsedProblemSetSlug), Right(parsedProblemSlug)) =>
            handlers.removeProblem(request, parsedProblemSetSlug, parsedProblemSlug)
    }
