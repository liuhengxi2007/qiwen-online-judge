package domains.problem.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import org.http4s.{Request, Response}
import org.http4s.dsl.Http4sDsl

final class ProblemHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  private def runAuthenticatedPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.Plain[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      registeredPlan.plan.execute(databaseSession, actor, input).flatMap(registeredPlan.toResponse)
    }

  private def runAuthenticatedPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.WithTransaction[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(connection, actor, input).flatMap(registeredPlan.toResponse)
      )
    }

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.Plain[Input, Output]
  ): IO[Response[IO]] =
    runAuthenticatedPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.WithTransaction[Input, Output]
  ): IO[Response[IO]] =
    runAuthenticatedPlan(request, input, registeredPlan)

  private def runDecodedAuthenticatedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.Plain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        registeredPlan.plan.execute(databaseSession, actor, toInput(body)).flatMap(registeredPlan.toResponse)
      )
    }

  private def runDecodedAuthenticatedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.WithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        databaseSession.withTransactionConnection(connection =>
          registeredPlan.plan.execute(connection, actor, toInput(body)).flatMap(registeredPlan.toResponse)
        )
      )
    }

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.Plain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedAuthenticatedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: ProblemHttpPlanRegistry.RegisteredPlan.WithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedAuthenticatedPlan(request, registeredPlan)(toInput)
