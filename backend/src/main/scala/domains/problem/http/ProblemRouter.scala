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
import org.http4s.dsl.io.*

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          ProblemCommands
            .listProblems(databaseSession, actor, PageRequest())
            .flatMap(response => Ok(ProblemHttpResponses.toProblemListResponse(response).asJson))
        }

      case request @ POST -> Root / "api" / "problems" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          for
            createRequest <- request.as[CreateProblemRequest]
            response <- ProblemCommands
              .createProblem(databaseSession, actor, createRequest)
              .flatMap(ProblemHttpResponses.mapCreateResult)
          yield response
        }

      case request @ GET -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemCommands
                .getProblemBySlug(databaseSession, actor, parsedProblemSlug)
                .flatMap(ProblemHttpResponses.mapGetResult)
            }

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemCommands
                .listProblemData(databaseSession, actor, parsedProblemSlug)
                .flatMap(ProblemHttpResponses.mapListDataResult)
            }

      case request @ GET -> Root / "api" / "problems" / problemSlug / "data" / filename =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
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

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / filename / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            ProblemDataFilename.parse(filename) match
              case Left(message) =>
                ProblemHttpResponses.validationErrorResponse(message)
              case Right(parsedFilename) =>
                AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
                  ProblemCommands
                    .deleteProblemData(databaseSession, actor, parsedProblemSlug, parsedFilename)
                    .flatMap(ProblemHttpResponses.mapDeleteDataResult)
                }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" / "clear" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemCommands
                .clearProblemData(databaseSession, actor, parsedProblemSlug)
                .flatMap(ProblemHttpResponses.mapClearDataResult)
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              for
                updateRequest <- request.as[UpdateProblemRequest]
                response <- ProblemCommands
                  .updateProblem(databaseSession, actor, parsedProblemSlug, updateRequest)
                  .flatMap(ProblemHttpResponses.mapUpdateResult)
              yield response
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "data" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              for
                updateDataRequest <- request.as[UpdateProblemDataRequest]
                response <- ProblemCommands
                  .updateProblemData(databaseSession, actor, parsedProblemSlug, updateDataRequest)
                  .flatMap(ProblemHttpResponses.mapUpdateDataResult)
              yield response
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemCommands
                .deleteProblem(databaseSession, actor, parsedProblemSlug)
                .flatMap(ProblemHttpResponses.mapDeleteResult)
            }
    }
