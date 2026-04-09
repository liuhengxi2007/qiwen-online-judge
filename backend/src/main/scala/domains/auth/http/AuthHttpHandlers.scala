package domains.auth.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.{AuthUserCommands, PasswordHasher, SessionStore, UsernameRules}
import domains.auth.model.{AuthUser, DisplayName, EmailAddress, LoginRequest, RegisterRequest, SiteManagerUser, UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest, Username}
import domains.auth.table.AuthUserTable
import domains.judge.application.JudgeConfig
import domains.judger.table.JudgerTable
import domains.usergroup.model.UserGroupSlug
import domains.usergroup.table.UserGroupTable
import io.circe.syntax.*
import org.http4s.{Request, Response}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class AuthHttpHandlers(
  databaseSession: DatabaseSession,
  sessionStore: SessionStore,
  sessionSupport: AuthHttpSessionSupport,
  judgeConfig: JudgeConfig
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

  def listJudgers(request: Request[IO]): IO[Response[IO]] =
    sessionSupport.withSiteManager(request) { _ =>
      databaseSession.withTransactionConnection { connection =>
        JudgerTable
          .listJudgers(connection, judgeConfig.heartbeatTimeoutMs)
          .flatMap(registeredJudgers => Ok(registeredJudgers.asJson))
      }
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
      val isOwnSettings = targetUsername.value == authenticatedActor.username.value

      if isOwnSettings then
        updateOwnUserSettings(request, authenticatedActor, targetUsername)
      else
        SiteManagerUser.from(authenticatedActor) match
          case None =>
            AuthHttpResponses.forbiddenResponse
          case Some(siteManagerActor) =>
            updateManagedUserSettings(request, siteManagerActor, targetUsername)
    }

  def deleteUser(request: Request[IO], targetUsername: Username): IO[Response[IO]] =
    sessionSupport.withAuthenticatedUser(request) { authenticatedActor =>
      AuthUserCommands
        .deleteUser(databaseSession, authenticatedActor, targetUsername)
        .flatMap(AuthHttpResponses.mapDeleteUserResult)
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
              existingUserGroup <- findConflictingUserGroup(connection, registerRequest.username)
              result <- existingUser match
                case Some(_) =>
                  AuthHttpResponses.usernameConflictResponse
                case None if existingUserGroup.nonEmpty =>
                  AuthHttpResponses.usernameConflictsWithUserGroupResponse
                case None =>
                  validateRegisterRequest(registerRequest) match
                    case Some(validationError) =>
                      AuthHttpResponses.validationErrorResponse(validationError)
                    case None =>
                      AuthUserTable
                        .insert(
                          connection,
                          username = registerRequest.username,
                          displayName = registerRequest.displayName,
                          email = registerRequest.email,
                          password = registerRequest.password
                        )
                        .flatMap(createdUser => loginCreatedUser(connection, createdUser))
            yield result
          }
    yield response

  private def validateRegisterRequest(request: RegisterRequest): Option[String] =
    validateDisplayName(request.displayName).orElse(validateEmail(request.email))

  private def validateDisplayName(displayName: DisplayName): Option[String] =
    val normalized = displayName.value.trim

    if normalized.isEmpty then Some("Display name is required.")
    else if normalized.length > 120 then Some("Display name must be at most 120 characters.")
    else None

  private def validateEmail(email: EmailAddress): Option[String] =
    val normalized = email.value.trim
    val emailPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".r

    if normalized.isEmpty then Some("Email is required.")
    else if normalized.length > 255 then Some("Email must be at most 255 characters.")
    else if emailPattern.matches(normalized) then None
    else Some("Please enter a valid email address.")

  private def findConflictingUserGroup(
    connection: java.sql.Connection,
    username: Username
  ): IO[Option[domains.usergroup.model.UserGroup]] =
    UserGroupSlug.parse(username.value) match
      case Left(_) => IO.pure(None)
      case Right(slug) => UserGroupTable.findBySlug(connection, slug)

  private def updateOwnUserSettings(
    request: Request[IO],
    authenticatedActor: AuthUser,
    targetUsername: Username
  ): IO[Response[IO]] =
    for
      updateRequest <- request.as[UpdateOwnSettingsRequest]
      result <- AuthUserCommands
        .updateUserSettings(
          databaseSession,
          targetUsername,
          AuthUserCommands.UpdateUserSettingsCommand.UpdateOwn(authenticatedActor, updateRequest)
        )
      _ <- revokePasswordChangedSessions(targetUsername, result)
      response <- AuthHttpResponses.mapUpdateUserSettingsResult(result)
      finalResponse <- clearCurrentSessionCookieIfNeeded(response, result)
    yield finalResponse

  private def updateManagedUserSettings(
    request: Request[IO],
    siteManagerActor: SiteManagerUser,
    targetUsername: Username
  ): IO[Response[IO]] =
    for
      updateRequest <- request.as[UpdateManagedUserSettingsRequest]
      result <- AuthUserCommands
        .updateUserSettings(
          databaseSession,
          targetUsername,
          AuthUserCommands.UpdateUserSettingsCommand.UpdateManaged(siteManagerActor, updateRequest)
        )
      _ <- revokePasswordChangedSessions(targetUsername, result)
      response <- AuthHttpResponses.mapUpdateUserSettingsResult(result)
    yield response

  private def revokePasswordChangedSessions(
    targetUsername: Username,
    result: AuthUserCommands.UpdateUserSettingsResult
  ): IO[Unit] =
    result match
      case AuthUserCommands.UpdateUserSettingsResult.Updated(_, true) =>
        sessionStore.deleteSessionsForUsername(targetUsername)
      case _ =>
        IO.unit

  private def clearCurrentSessionCookieIfNeeded(
    response: Response[IO],
    result: AuthUserCommands.UpdateUserSettingsResult
  ): IO[Response[IO]] =
    result match
      case AuthUserCommands.UpdateUserSettingsResult.Updated(_, true) =>
        IO.pure(response.addCookie(AuthHttpResponses.clearedSessionCookie))
      case _ =>
        IO.pure(response)

  private def loginCreatedUser(connection: java.sql.Connection, createdUser: AuthUser): IO[Response[IO]] =
    sessionStore.createSessionInConnection(connection, createdUser.username).flatMap { sessionToken =>
      Created(AuthHttpResponses.toLoginResponse(createdUser, "Registration successful").asJson)
        .map(_.addCookie(AuthHttpResponses.sessionCookie(sessionToken)))
    }
