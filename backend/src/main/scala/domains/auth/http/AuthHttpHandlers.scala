package domains.auth.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.{AuthUserCommands, PasswordHasher, SessionStore, UsernameRules}
import domains.auth.model.{AuthUser, LoginRequest, RegisterRequest, SiteManagerUser, UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest, Username}
import domains.auth.table.AuthUserTable
import io.circe.syntax.*
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.Http4sDsl
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class AuthHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  sessionSupport: AuthHttpSessionSupport
)(using dsl: Http4sDsl[IO]):

  import dsl.*

  private val logger = Slf4jLogger.getLogger[IO]

  def session(request: Request[IO]): IO[Response[IO]] =
    sessionSupport.withAuthenticatedUser(request) { user =>
      Ok(AuthHttpResponses.toSessionResponse(user).asJson)
    }

  def logout(request: Request[IO]): IO[Response[IO]] =
    sessionSupport.currentSessionToken(request) match
      case Some(token) =>
        sessionStore.deleteSession(token) *> AuthHttpResponses.loggedOutResponse(AuthHttpResponses.clearedSessionCookie)
      case None =>
        AuthHttpResponses.loggedOutResponse(AuthHttpResponses.clearedSessionCookie)

  def listUsers(request: Request[IO]): IO[Response[IO]] =
    sessionSupport.withSiteManager(request) { siteManagerActor =>
      for
        _ <- logger.info("AuthRouter received user list request")
        users <- databaseSession.withTransactionConnection(connection =>
          AuthUserTable.listUsers(connection, siteManagerActor)
        )
        response <- Ok(users.asJson)
      yield response
    }

  def getUserSettings(request: Request[IO], targetUsername: Username): IO[Response[IO]] =
    sessionSupport.withAuthenticatedUser(request) { authenticatedActor =>
      AuthUserCommands
        .getUserSettings(databaseSession, authenticatedActor, targetUsername)
        .flatMap(AuthHttpResponses.mapGetUserSettingsResult)
    }

  def updateUserPermissions(
    request: Request[IO],
    targetUsername: Username
  ): IO[Response[IO]] =
    sessionSupport.withAuthenticatedUser(request) { authenticatedActor =>
      for
        permissionsRequest <- request.as[UpdateUserPermissionsRequest]
        response <- AuthUserCommands
          .updateUserPermissions(
            databaseSession,
            authenticatedActor,
            targetUsername,
            permissionsRequest
          )
          .flatMap(AuthHttpResponses.mapUpdateUserPermissionsResult)
      yield response
    }

  def updateUserSettings(
    request: Request[IO],
    targetUsername: Username
  ): IO[Response[IO]] =
    sessionSupport.withAuthenticatedUser(request) { authenticatedActor =>
      val isOwnSettings = targetUsername.value.equalsIgnoreCase(authenticatedActor.username.value)

      if isOwnSettings then
        updateOwnUserSettings(request, authenticatedActor, targetUsername)
      else
        SiteManagerUser.from(authenticatedActor) match
          case None =>
            AuthHttpResponses.forbiddenResponse
          case Some(siteManagerActor) =>
            updateManagedUserSettings(request, siteManagerActor, targetUsername)
    }

  def login(request: Request[IO]): IO[Response[IO]] =
    for
      loginRequest <- request.as[LoginRequest]
      _ <- logger.info(s"AuthRouter received login request for ${loginRequest.username.value}")
      user <- databaseSession.withTransactionConnection(connection =>
        AuthUserTable.findByUsername(connection, loginRequest.username)
      )
      response <- user match
        case Some(foundUser) =>
          PasswordHasher.verifyPassword(loginRequest.password, foundUser.passwordHash).flatMap {
            case true =>
              sessionStore.createSession(foundUser.username).flatMap(sessionToken =>
                Ok(AuthHttpResponses.toLoginResponse(foundUser, "Login successful").asJson)
                  .map(_.addCookie(AuthHttpResponses.sessionCookie(sessionToken)))
              )
            case false =>
              AuthHttpResponses.invalidCredentialsResponse
          }
        case None =>
          AuthHttpResponses.invalidCredentialsResponse
    yield response

  def register(request: Request[IO]): IO[Response[IO]] =
    for
      registerRequest <- request.as[RegisterRequest]
      _ <- logger.info(s"AuthRouter received register request for ${registerRequest.username.value}")
      response <- UsernameRules.validate(registerRequest.username) match
        case Some(validationError) =>
          AuthHttpResponses.validationErrorResponse(validationError)
        case None =>
          databaseSession.withTransactionConnection { connection =>
            for
              existingUser <- AuthUserTable.findByUsername(connection, registerRequest.username)
              result <- existingUser match
                case Some(_) =>
                  AuthHttpResponses.usernameConflictResponse
                case None =>
                  AuthUserTable
                    .insert(
                      connection,
                      username = registerRequest.username,
                      displayName = registerRequest.displayName,
                      email = registerRequest.email,
                      password = registerRequest.password
                    )
                    .flatMap(createdUser => loginCreatedUser(createdUser))
            yield result
          }
    yield response

  private def updateOwnUserSettings(
    request: Request[IO],
    authenticatedActor: AuthUser,
    targetUsername: Username
  ): IO[Response[IO]] =
    for
      updateRequest <- request.as[UpdateOwnSettingsRequest]
      response <- AuthUserCommands
        .updateUserSettings(
          databaseSession,
          targetUsername,
          AuthUserCommands.UpdateUserSettingsCommand.UpdateOwn(authenticatedActor, updateRequest)
        )
        .flatMap(AuthHttpResponses.mapUpdateUserSettingsResult)
    yield response

  private def updateManagedUserSettings(
    request: Request[IO],
    siteManagerActor: SiteManagerUser,
    targetUsername: Username
  ): IO[Response[IO]] =
    for
      updateRequest <- request.as[UpdateManagedUserSettingsRequest]
      response <- AuthUserCommands
        .updateUserSettings(
          databaseSession,
          targetUsername,
          AuthUserCommands.UpdateUserSettingsCommand.UpdateManaged(siteManagerActor, updateRequest)
        )
        .flatMap(AuthHttpResponses.mapUpdateUserSettingsResult)
    yield response

  private def loginCreatedUser(createdUser: AuthUser): IO[Response[IO]] =
    sessionStore.createSession(createdUser.username).flatMap { sessionToken =>
      Created(AuthHttpResponses.toLoginResponse(createdUser, "Registration successful").asJson)
        .map(_.addCookie(AuthHttpResponses.sessionCookie(sessionToken)))
    }
