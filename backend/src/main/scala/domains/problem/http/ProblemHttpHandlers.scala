package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemSlug, UpdateProblemDataRequest, UpdateProblemRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class ProblemHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  def listProblems(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .listProblems(databaseSession, actor, PageRequest())
        .flatMap(response => Ok(ProblemHttpResponses.toProblemListResponse(response).asJson))
    }

  def createProblem(request: Request[IO]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        createRequest <- request.as[CreateProblemRequest]
        response <- ProblemCommands
          .createProblem(databaseSession, actor, createRequest)
          .flatMap(ProblemHttpResponses.mapCreateResult)
      yield response
    }

  def getProblem(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .getProblemBySlug(databaseSession, actor, parsedProblemSlug)
        .flatMap(ProblemHttpResponses.mapGetResult)
    }

  def listProblemData(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .listProblemData(databaseSession, actor, parsedProblemSlug)
        .flatMap(ProblemHttpResponses.mapListDataResult)
    }

  def downloadProblemData(
    request: Request[IO],
    parsedProblemSlug: ProblemSlug,
    parsedFilename: ProblemDataFilename
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .authorizeProblemDataDownload(databaseSession, actor, parsedProblemSlug)
        .flatMap {
          case ProblemCommands.AuthorizeProblemDataDownloadResult.Authorized =>
            ProblemHttpResponses.downloadDataResponse(parsedProblemSlug, parsedFilename)
          case other =>
            ProblemHttpResponses.mapAuthorizeDownloadResult(other)
        }
    }

  def deleteProblemData(
    request: Request[IO],
    parsedProblemSlug: ProblemSlug,
    parsedFilename: ProblemDataFilename
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .deleteProblemData(databaseSession, actor, parsedProblemSlug, parsedFilename)
        .flatMap(ProblemHttpResponses.mapDeleteDataResult)
    }

  def clearProblemData(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .clearProblemData(databaseSession, actor, parsedProblemSlug)
        .flatMap(ProblemHttpResponses.mapClearDataResult)
    }

  def updateProblem(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        updateRequest <- request.as[UpdateProblemRequest]
        response <- ProblemCommands
          .updateProblem(databaseSession, actor, parsedProblemSlug, updateRequest)
          .flatMap(ProblemHttpResponses.mapUpdateResult)
      yield response
    }

  def updateProblemData(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      for
        updateDataRequest <- request.as[UpdateProblemDataRequest]
        response <- ProblemCommands
          .updateProblemData(databaseSession, actor, parsedProblemSlug, updateDataRequest)
          .flatMap(ProblemHttpResponses.mapUpdateDataResult)
      yield response
    }

  def deleteProblem(request: Request[IO], parsedProblemSlug: ProblemSlug): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      ProblemCommands
        .deleteProblem(databaseSession, actor, parsedProblemSlug)
        .flatMap(ProblemHttpResponses.mapDeleteResult)
    }
