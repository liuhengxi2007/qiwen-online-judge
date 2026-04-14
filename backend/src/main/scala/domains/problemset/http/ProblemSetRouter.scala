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
import org.http4s.dsl.io.*

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          ProblemSetCommands
            .listProblemSets(databaseSession, actor, PageRequest())
            .flatMap(response => Ok(ProblemSetHttpResponses.toProblemSetListResponse(response).asJson))
        }

      case request @ GET -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemSetCommands
                .getProblemSetBySlug(databaseSession, actor, parsedProblemSetSlug)
                .flatMap(ProblemSetHttpResponses.mapGetResult)
            }

      case request @ POST -> Root / "api" / "problem-sets" =>
        AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
          for
            createRequest <- request.as[CreateProblemSetRequest]
            response <- ProblemSetCommands
              .createProblemSet(databaseSession, actor, createRequest)
              .flatMap(ProblemSetHttpResponses.mapCreateResult)
          yield response
        }

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              for
                addRequest <- request.as[AddProblemToProblemSetRequest]
                response <- ProblemSetCommands
                  .addProblemToProblemSet(databaseSession, actor, parsedProblemSetSlug, addRequest)
                  .flatMap(ProblemSetHttpResponses.mapAddProblemResult)
              yield response
            }

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              for
                updateRequest <- request.as[UpdateProblemSetRequest]
                response <- ProblemSetCommands
                  .updateProblemSet(databaseSession, actor, parsedProblemSetSlug, updateRequest)
                  .flatMap(ProblemSetHttpResponses.mapUpdateResult)
              yield response
            }

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "delete" =>
        ProblemSetSlug.parse(problemSetSlug) match
          case Left(message) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case Right(parsedProblemSetSlug) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemSetCommands
                .deleteProblemSet(databaseSession, actor, parsedProblemSetSlug)
                .flatMap(ProblemSetHttpResponses.mapDeleteResult)
            }

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" / linkedProblemSlug / "remove" =>
        (ProblemSetSlug.parse(problemSetSlug), ProblemSlug.parse(linkedProblemSlug)) match
          case (Left(message), _) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (_, Left(message)) =>
            ProblemSetHttpResponses.validationErrorResponse(message)
          case (Right(parsedProblemSetSlug), Right(parsedProblemSlug)) =>
            AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
              ProblemSetCommands
                .removeProblemFromProblemSet(databaseSession, actor, parsedProblemSetSlug, parsedProblemSlug)
                .flatMap(ProblemSetHttpResponses.mapRemoveProblemResult)
            }
    }
