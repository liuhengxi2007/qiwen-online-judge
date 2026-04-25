package domains.user.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.AuthHttpSessionSupport
import domains.auth.model.{AuthUser, SiteManagerUser, Username}
import domains.user.http.UserHttpPlanRegistry.RegisteredPlan
import domains.user.model.{UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl

import java.sql.Connection

final class UserHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  private val context = UserHttpContext(databaseSession, sessionStore)

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.AuthenticatedPlain[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      registeredPlan.plan.execute(context, actor, input).flatMap(registeredPlan.toResponse)
    }

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.SiteManagerPlain[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      registeredPlan.plan.execute(context, actor, input).flatMap(registeredPlan.toResponse)
    }

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.AuthenticatedWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(context, connection, actor, input).flatMap(registeredPlan.toResponse)
      )
    }

  def execute[Input, Output](
    request: Request[IO],
    input: Input,
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      databaseSession.withTransactionConnection(connection =>
        registeredPlan.plan.execute(context, connection, actor, input).flatMap(registeredPlan.toResponse)
      )
    }

  def executeDecoded[Body, Input, Output](
    request: Request[IO],
    registeredPlan: RegisteredPlan.SiteManagerWithTransaction[Input, Output]
  )(
    toInput: Body => Input
  )(using org.http4s.EntityDecoder[IO, Body]): IO[Response[IO]] =
    AuthHttpSessionSupport.withSiteManager(databaseSession, sessionStore, request) { actor =>
      request.as[Body].flatMap(body =>
        databaseSession.withTransactionConnection(connection =>
          registeredPlan.plan.execute(context, connection, actor, toInput(body)).flatMap(registeredPlan.toResponse)
        )
      )
    }

  def executeUserSettingsUpdate(
    request: Request[IO],
    targetUsername: Username
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      if targetUsername.value == actor.username.value then
        runOwnSettingsUpdate(request, targetUsername, actor)
      else
        SiteManagerUser.from(actor) match
          case Some(siteManagerActor) =>
            runManagedSettingsUpdate(request, targetUsername, siteManagerActor)
          case None =>
            UserHttpResponses.validationErrorResponse("Site manager permission required.")
    }

  private def runOwnSettingsUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: AuthUser
  ): IO[Response[IO]] =
    request.as[UpdateOwnSettingsRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        UserHttpPlanDefinitions.updateOwnSettings.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(UserHttpPlanDefinitions.updateOwnSettings.toResponse)
      )
    }

  private def runManagedSettingsUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: SiteManagerUser
  ): IO[Response[IO]] =
    request.as[UpdateManagedUserSettingsRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        UserHttpPlanDefinitions.updateManagedSettings.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(UserHttpPlanDefinitions.updateManagedSettings.toResponse)
      )
    }
