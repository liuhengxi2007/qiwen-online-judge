package domains.problemset.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.problemset.application.ProblemSetCommands
import domains.problem.model.ProblemSlug
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetSlug, UpdateProblemSetRequest}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new ProblemSetHttpHandlers(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" =>
        handlers.execute(request, (), ProblemSetHttpPlanDefinitions.listProblemSets)

      case request @ GET -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.execute(request, parsedProblemSetSlug, ProblemSetHttpPlanDefinitions.getProblemSet)

      case request @ POST -> Root / "api" / "problem-sets" =>
        handlers.executeDecoded[CreateProblemSetRequest, CreateProblemSetRequest, ProblemSetCommands.CreateProblemSetResult](
          request,
          ProblemSetHttpPlanDefinitions.createProblemSet
        )(identity)

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

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.executeDecoded[
              UpdateProblemSetRequest,
              (ProblemSetSlug, UpdateProblemSetRequest),
              ProblemSetCommands.UpdateProblemSetResult
            ](
              request,
              ProblemSetHttpPlanDefinitions.updateProblemSet
            )(updateRequest => (parsedProblemSetSlug, updateRequest))

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "delete" =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            handlers.execute(request, parsedProblemSetSlug, ProblemSetHttpPlanDefinitions.deleteProblemSet)

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" / linkedProblemSlug / "remove" =>
        (ProblemSetSlug.parse(problemSetSlug), ProblemSlug.parse(linkedProblemSlug)) match
          case (Left(message), _) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (_, Left(message)) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (Right(parsedProblemSetSlug), Right(parsedProblemSlug)) =>
            handlers.execute(request, (parsedProblemSetSlug, parsedProblemSlug), ProblemSetHttpPlanDefinitions.removeProblem)
    }
