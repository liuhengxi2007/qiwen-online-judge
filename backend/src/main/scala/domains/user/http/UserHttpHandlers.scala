package domains.user.http

import domains.user.http.mapper.UserHttpResponseMappers
import domains.user.http.codec.UserHttpCodecs.given



import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.auth.model.{AuthUser, SiteManagerUser}
import domains.user.model.Username
import domains.user.http.UserHttpPlanRegistry.RegisteredPlan
import domains.user.model.request.{UpdateManagedUserPreferencesRequest, UpdateManagedUserProfileRequest, UpdateOwnPreferencesRequest, UpdateOwnProfileRequest}
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl


final class UserHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore
)(using dsl: Http4sDsl[IO]):

  private val context = UserHttpContext(databaseSession)

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

  def executeUserSettingsProfileUpdate(
    request: Request[IO],
    targetUsername: Username
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      if targetUsername.value == actor.username.value then
        runOwnProfileUpdate(request, targetUsername, actor)
      else
        SiteManagerUser.from(actor) match
          case Some(siteManagerActor) =>
            runManagedProfileUpdate(request, targetUsername, siteManagerActor)
          case None =>
            UserHttpResponseMappers.validationErrorResponse("Site manager permission required.")
    }

  def executeUserSettingsPreferencesUpdate(
    request: Request[IO],
    targetUsername: Username
  ): IO[Response[IO]] =
    AuthHttpSessionSupport.withAuthenticatedUser(databaseSession, sessionStore, request) { actor =>
      if targetUsername.value == actor.username.value then
        runOwnPreferencesUpdate(request, targetUsername, actor)
      else
        SiteManagerUser.from(actor) match
          case Some(siteManagerActor) =>
            runManagedPreferencesUpdate(request, targetUsername, siteManagerActor)
          case None =>
            UserHttpResponseMappers.validationErrorResponse("Site manager permission required.")
    }

  private def runOwnProfileUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: AuthUser
  ): IO[Response[IO]] =
    request.as[UpdateOwnProfileRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        UserHttpPlanDefinitions.updateOwnProfile.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(UserHttpPlanDefinitions.updateOwnProfile.toResponse)
      )
    }

  private def runOwnPreferencesUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: AuthUser
  ): IO[Response[IO]] =
    request.as[UpdateOwnPreferencesRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        UserHttpPlanDefinitions.updateOwnPreferences.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(UserHttpPlanDefinitions.updateOwnPreferences.toResponse)
      )
    }

  private def runManagedProfileUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: SiteManagerUser
  ): IO[Response[IO]] =
    request.as[UpdateManagedUserProfileRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        UserHttpPlanDefinitions.updateManagedProfile.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(UserHttpPlanDefinitions.updateManagedProfile.toResponse)
      )
    }

  private def runManagedPreferencesUpdate(
    request: Request[IO],
    targetUsername: Username,
    actor: SiteManagerUser
  ): IO[Response[IO]] =
    request.as[UpdateManagedUserPreferencesRequest].flatMap { body =>
      databaseSession.withTransactionConnection(connection =>
        UserHttpPlanDefinitions.updateManagedPreferences.plan
          .execute(context, connection, actor, (targetUsername, body))
          .flatMap(UserHttpPlanDefinitions.updateManagedPreferences.toResponse)
      )
    }
