package domains.problemset.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.problemset.application.ProblemSetCommands
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetSlug}
import domains.shared.model.PageRequest
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*

object ProblemSetRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    val sessionSupport = new AuthHttpSessionSupport(databaseSession, sessionStore)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problem-sets" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          ProblemSetCommands
            .listProblemSets(databaseSession, actor, PageRequest())
            .flatMap(response => Ok(response.asJson))
        }

      case request @ GET -> Root / "api" / "problem-sets" / problemSetSlug =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          ProblemSetCommands
            .getProblemSetBySlug(databaseSession, actor, ProblemSetSlug(problemSetSlug))
            .flatMap(ProblemSetHttpResponses.mapGetResult)
        }

      case request @ POST -> Root / "api" / "problem-sets" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          for
            createRequest <- request.as[CreateProblemSetRequest]
            response <- ProblemSetCommands
              .createProblemSet(databaseSession, actor, createRequest)
              .flatMap(ProblemSetHttpResponses.mapCreateResult)
          yield response
        }

      case request @ POST -> Root / "api" / "problem-sets" / problemSetSlug / "problems" =>
        sessionSupport.withAuthenticatedUser(request) { actor =>
          for
            addRequest <- request.as[AddProblemToProblemSetRequest]
            response <- ProblemSetCommands
              .addProblemToProblemSet(databaseSession, actor, ProblemSetSlug(problemSetSlug), addRequest)
              .flatMap(ProblemSetHttpResponses.mapAddProblemResult)
          yield response
        }
    }
