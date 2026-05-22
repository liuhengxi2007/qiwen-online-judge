package shared.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan
import org.http4s.{Request, Response}
import org.http4s.dsl.Http4sDsl

final class AuthenticatedHttpExecutor(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.Plain[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      registeredPlan.plan.execute(databaseSession, actor, input).flatMap(registeredPlan.toResponse)
    }

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.WithTransaction[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(connection, actor, input).flatMap(registeredPlan.toResponse)
      )
    }

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.Plain[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.WithTransaction[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.Plain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        registeredPlan.plan.execute(databaseSession, actor, toInput(body)).flatMap(registeredPlan.toResponse)
      )
    }

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.WithTransaction[Input, Output]
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
    registeredPlan: RegisteredPlan.Plain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.WithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)
