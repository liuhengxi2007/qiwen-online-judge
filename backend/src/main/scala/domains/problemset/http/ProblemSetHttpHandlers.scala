package domains.problemset.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problem.model.ProblemSlug
import domains.problemset.application.ProblemSetCommands
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetSlug, UpdateProblemSetRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class ProblemSetHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  def listProblemSets(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemSetCommands
        .listProblemSets(databaseSession, actor, PageRequest())
        .flatMap(response => Ok(ProblemSetHttpResponses.toProblemSetListResponse(response).asJson))
    }

  def getProblemSet(request: Request[IO], parsedProblemSetSlug: ProblemSetSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemSetCommands
        .getProblemSetBySlug(databaseSession, actor, parsedProblemSetSlug)
        .flatMap(ProblemSetHttpResponses.mapGetResult)
    }

  def createProblemSet(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        createRequest <- request.as[CreateProblemSetRequest]
        response <- ProblemSetCommands
          .createProblemSet(databaseSession, actor, createRequest)
          .flatMap(ProblemSetHttpResponses.mapCreateResult)
      yield response
    }

  def addProblem(request: Request[IO], parsedProblemSetSlug: ProblemSetSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        addRequest <- request.as[AddProblemToProblemSetRequest]
        response <- ProblemSetCommands
          .addProblemToProblemSet(databaseSession, actor, parsedProblemSetSlug, addRequest)
          .flatMap(ProblemSetHttpResponses.mapAddProblemResult)
      yield response
    }

  def updateProblemSet(request: Request[IO], parsedProblemSetSlug: ProblemSetSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        updateRequest <- request.as[UpdateProblemSetRequest]
        response <- ProblemSetCommands
          .updateProblemSet(databaseSession, actor, parsedProblemSetSlug, updateRequest)
          .flatMap(ProblemSetHttpResponses.mapUpdateResult)
      yield response
    }

  def deleteProblemSet(request: Request[IO], parsedProblemSetSlug: ProblemSetSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemSetCommands
        .deleteProblemSet(databaseSession, actor, parsedProblemSetSlug)
        .flatMap(ProblemSetHttpResponses.mapDeleteResult)
    }

  def removeProblem(
    request: Request[IO],
    parsedProblemSetSlug: ProblemSetSlug,
    parsedProblemSlug: ProblemSlug
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemSetCommands
        .removeProblemFromProblemSet(databaseSession, actor, parsedProblemSetSlug, parsedProblemSlug)
        .flatMap(ProblemSetHttpResponses.mapRemoveProblemResult)
    }
