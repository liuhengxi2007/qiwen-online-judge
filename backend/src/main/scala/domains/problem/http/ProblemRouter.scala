package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problem.application.ProblemCommands
import domains.problem.model.{CreateProblemRequest, ProblemSlug, UpdateProblemRequest}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*

object ProblemRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val sessionSupport = new AuthHttpSessionSupport(databaseSession, sessionStore)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          ProblemCommands
            .listProblems(databaseSession, actor, PageRequest())
            .flatMap(response => Ok(ProblemHttpResponses.toProblemListResponse(response).asJson))
        }

      case request @ POST -> Root / "api" / "problems" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
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
            sessionSupport.withAuthenticatedUser(request) { actor =>
              ProblemCommands
                .getProblemBySlug(databaseSession, actor, parsedProblemSlug)
                .flatMap(ProblemHttpResponses.mapGetResult)
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            sessionSupport.withAuthenticatedUser(request) { actor =>
              for
                updateRequest <- request.as[UpdateProblemRequest]
                response <- ProblemCommands
                  .updateProblem(databaseSession, actor, parsedProblemSlug, updateRequest)
                  .flatMap(ProblemHttpResponses.mapUpdateResult)
              yield response
            }

      case request @ POST -> Root / "api" / "problems" / problemSlug / "delete" =>
        ProblemSlug.parse(problemSlug) match
          case Left(message) =>
            ProblemHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSlug) =>
            sessionSupport.withAuthenticatedUser(request) { actor =>
              ProblemCommands
                .deleteProblem(databaseSession, actor, parsedProblemSlug)
                .flatMap(ProblemHttpResponses.mapDeleteResult)
            }
    }
