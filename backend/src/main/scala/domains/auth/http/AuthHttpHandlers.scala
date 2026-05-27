package domains.auth.http



import domains.auth.http.utils.AuthHttpSessionSupport
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpPlanRegistry.RegisteredPlan
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.http.mapper.AuthHttpResponseMappers
import domains.auth.objects.{AuthUser, SiteManagerUser}
import domains.auth.objects.request.{UpdateManagedUserAccountRequest, UpdateOwnAccountRequest}
import domains.user.objects.Username
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

final class AuthHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  private val context = AuthHttpContext(databaseSession, sessionStore)

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

  def executeDecoded[Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicPlain[Input, Output]
  )(using org.http4s.EntityDecoder[IO, Input]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)((body: Input) => body)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.PublicWithTransaction[Input, Output]
  )(using org.http4s.EntityDecoder[IO, Input]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)((body: Input) => body)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  )(using org.http4s.EntityDecoder[IO, Input]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)((body: Input) => body)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  )(using org.http4s.EntityDecoder[IO, Input]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)((body: Input) => body)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  )(using org.http4s.EntityDecoder[IO, Input]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)((body: Input) => body)

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)(toInput)

  def executeDecoded[Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  )(using org.http4s.EntityDecoder[IO, Input]): IO[Response[IO]] =
    runDecodedPlan(request, registeredPlan)((body: Input) => body)

  def executeAccountUpdate(
    request: Request[IO],
    targetUsername: Username
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      if targetUsername.value == actor.username.value then
        runOwnAccountUpdate(request, targetUsername, actor)
      else
        SiteManagerUser.from(actor) match
          case Some(siteManagerActor) =>
            runManagedAccountUpdate(request, targetUsername, siteManagerActor)
          case None =>
            AuthHttpResponseMappers.validationErrorResponse("Site manager permission required.")
    }

  private def runOwnAccountUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: AuthUser
  ): IO[Response[IO]] =
    request.as[UpdateOwnAccountRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        AuthHttpPlanDefinitions.updateOwnAccount.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(AuthHttpPlanDefinitions.updateOwnAccount.toResponse)
      )
    }

  private def runManagedAccountUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: SiteManagerUser
  ): IO[Response[IO]] =
    request.as[UpdateManagedUserAccountRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        AuthHttpPlanDefinitions.updateManagedAccount.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(AuthHttpPlanDefinitions.updateManagedAccount.toResponse)
      )
    }
