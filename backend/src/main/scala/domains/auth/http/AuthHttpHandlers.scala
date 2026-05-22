package domains.auth.http



import domains.auth.http.utils.AuthHttpSessionSupport
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.judge.application.JudgeConfig
import domains.auth.http.AuthHttpPlanRegistry.RegisteredPlan
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class AuthHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  judgeConfig: JudgeConfig
)(using dsl: Http4sDsl[IO]):

  private val context = AuthHttpContext(databaseSession, sessionStore, judgeConfig)

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.PublicPlain[Input, Output]
  ): IO[Response[IO]] =
    registeredPlan.plan.execute(context, input).flatMap(registeredPlan.toResponse)

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.PublicWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    context.databaseSession.withTransactionConnection(connection =>
      registeredPlan.plan.execute(context, connection, input).flatMap(registeredPlan.toResponse)
    )

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      registeredPlan.plan.execute(context, actor, input).flatMap(registeredPlan.toResponse)
    }

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      context.databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(context, connection, actor, input).flatMap(registeredPlan.toResponse)
      )
    }

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      registeredPlan.plan.execute(context, actor, input).flatMap(registeredPlan.toResponse)
    }

  private def runPlan[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      context.databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(context, connection, actor, input).flatMap(registeredPlan.toResponse)
      )
    }

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.PublicPlain[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.PublicWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    runPlan(request, input, registeredPlan)

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    request.as[Body].flatMap(body =>
      registeredPlan.plan.execute(context, toInput(body)).flatMap(registeredPlan.toResponse)
    )

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    request.as[Body].flatMap(body =>
      context.databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(context, connection, toInput(body)).flatMap(registeredPlan.toResponse)
      )
    )

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        registeredPlan.plan.execute(context, actor, toInput(body)).flatMap(registeredPlan.toResponse)
      )
    }

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        context.databaseSession.withTransactionConnection(connection =>
          registeredPlan.plan.execute(context, connection, actor, toInput(body)).flatMap(registeredPlan.toResponse)
        )
      )
    }

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        registeredPlan.plan.execute(context, actor, toInput(body)).flatMap(registeredPlan.toResponse)
      )
    }

  private def runDecodedPlan[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        context.databaseSession.withTransactionConnection(connection =>
          registeredPlan.plan.execute(context, connection, actor, toInput(body)).flatMap(registeredPlan.toResponse)
        )
      )
    }

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)
