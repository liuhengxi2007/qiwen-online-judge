package routes

import auth.{AuthUserCommands, PasswordHasher, SessionStore, UsernameRules}
import cats.effect.IO
import database.DatabaseSession
import io.circe.syntax.*
import objects.{AuthUser, AuthUserListItem, ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, SessionResponse, SiteManagerUser, UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest, Username}
import org.http4s.{HttpRoutes, Request, Response, ResponseCookie, SameSite, Status}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tables.AuthUserTable

object AuthRouter:

  private val logger = Slf4jLogger.getLogger[IO]
  private val sessionCookieName = "qiwen_session"
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    def invalidCredentialsResponse: IO[Response[IO]] =
      IO.pure(
        Response[IO](status = Status.Unauthorized)
          .withEntity(ErrorResponse("Invalid username or password.").asJson)
      )

    def invalidCurrentPasswordResponse: IO[Response[IO]] =
      IO.pure(
        Response[IO](status = Status.Unauthorized)
          .withEntity(ErrorResponse("Current password is incorrect.").asJson)
      )

    def unauthorizedResponse: IO[Response[IO]] =
      IO.pure(
        Response[IO](status = Status.Unauthorized)
          .withEntity(ErrorResponse("Authentication required.").asJson)
      )

    def forbiddenResponse: IO[Response[IO]] =
      IO.pure(
        Response[IO](status = Status.Forbidden)
          .withEntity(ErrorResponse("Site manager permission required.").asJson)
      )

    def protectedAdminResponse: IO[Response[IO]] =
      IO.pure(
        Response[IO](status = Status.Forbidden)
          .withEntity(ErrorResponse("The admin account permissions cannot be modified.").asJson)
      )

    def sessionCookie(token: String): ResponseCookie =
      ResponseCookie(
        name = sessionCookieName,
        content = token,
        path = Some("/"),
        httpOnly = true,
        sameSite = Some(SameSite.Lax)
      )

    val clearedSessionCookie: ResponseCookie =
      ResponseCookie(
        name = sessionCookieName,
        content = "",
        path = Some("/"),
        httpOnly = true,
        sameSite = Some(SameSite.Lax),
        maxAge = Some(0L)
      )

    def currentSessionToken(request: Request[IO]): Option[String] =
      request.cookies.find(_.name == sessionCookieName).map(_.content)

    def authenticatedUser(request: Request[IO]): IO[Option[AuthUser]] =
      currentSessionToken(request) match
        case Some(token) =>
          sessionStore.lookupUsername(token).flatMap {
            case Some(username) =>
              databaseSession.withTransactionConnection(connection =>
                AuthUserTable.findByUsername(connection, username)
              )
            case None =>
              IO.pure(None)
          }
        case None =>
          IO.pure(None)

    def withAuthenticatedUser(request: Request[IO])(handle: AuthUser => IO[Response[IO]]): IO[Response[IO]] =
      authenticatedUser(request).flatMap {
        case Some(user) => handle(user)
        case None => unauthorizedResponse
      }

    def withSiteManager(request: Request[IO])(handle: SiteManagerUser => IO[Response[IO]]): IO[Response[IO]] =
      withAuthenticatedUser(request) { user =>
        SiteManagerUser.from(user) match
          case Some(siteManagerUser) => handle(siteManagerUser)
          case None => forbiddenResponse
      }

    def userSettingsResponse(user: AuthUser): SessionResponse =
      SessionResponse(
        displayName = user.displayName,
        username = user.username,
        email = user.email,
        siteManager = user.siteManager,
        problemManager = user.problemManager
      )

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "auth" / "session" =>
        withAuthenticatedUser(request) { user =>
          Ok(userSettingsResponse(user).asJson)
        }

      case request @ POST -> Root / "api" / "auth" / "logout" =>
        currentSessionToken(request) match
          case Some(token) =>
            sessionStore.deleteSession(token) *> Ok(ErrorResponse("Logged out.").asJson).map(_.addCookie(clearedSessionCookie))
          case None =>
            Ok(ErrorResponse("Logged out.").asJson).map(_.addCookie(clearedSessionCookie))

      case request @ GET -> Root / "api" / "auth" / "users" =>
        withSiteManager(request) { siteManagerActor =>
          for
            _ <- logger.info("AuthRouter received user list request")
            users <- databaseSession.withTransactionConnection(connection =>
              AuthUserTable.listUsers(connection, siteManagerActor)
            )
            response <- Ok(users.asJson)
          yield response
        }

      case request @ GET -> Root / "api" / "auth" / "users" / targetUsername / "settings" =>
        withAuthenticatedUser(request) { authenticatedActor =>
          AuthUserCommands.getUserSettings(
            databaseSession,
            authenticatedActor,
            Username(targetUsername)
          ).flatMap {
            case AuthUserCommands.GetUserSettingsResult.Forbidden =>
              forbiddenResponse
            case AuthUserCommands.GetUserSettingsResult.NotFound =>
              NotFound(ErrorResponse("User not found.").asJson)
            case AuthUserCommands.GetUserSettingsResult.Found(targetUser) =>
              Ok(userSettingsResponse(targetUser).asJson)
          }
        }

      case request @ POST -> Root / "api" / "auth" / "users" / targetUsername / "permissions" =>
        withAuthenticatedUser(request) { authenticatedActor =>
          for
            permissionsRequest <- request.as[UpdateUserPermissionsRequest]
            response <- AuthUserCommands.updateUserPermissions(
              databaseSession,
              authenticatedActor,
              Username(targetUsername),
              permissionsRequest
            ).flatMap {
              case AuthUserCommands.UpdateUserPermissionsResult.Forbidden =>
                forbiddenResponse
              case AuthUserCommands.UpdateUserPermissionsResult.ProtectedAdmin =>
                protectedAdminResponse
              case AuthUserCommands.UpdateUserPermissionsResult.NotFound =>
                NotFound(ErrorResponse("User not found.").asJson)
              case AuthUserCommands.UpdateUserPermissionsResult.Updated(user) =>
                Ok(
                  AuthUserListItem(
                    username = user.username,
                    displayName = user.displayName,
                    email = user.email,
                    siteManager = user.siteManager,
                    problemManager = user.problemManager
                  ).asJson
                )
            }
          yield response
        }

      case request @ POST -> Root / "api" / "auth" / "users" / targetUsername / "settings" =>
        withAuthenticatedUser(request) { authenticatedActor =>
          val isOwnSettings = targetUsername.equalsIgnoreCase(authenticatedActor.username.value)
          val target = Username(targetUsername)

          if isOwnSettings then
            for
              updateRequest <- request.as[UpdateOwnSettingsRequest]
              response <- AuthUserCommands.updateUserSettings(
                databaseSession,
                target,
                AuthUserCommands.UpdateUserSettingsCommand.UpdateOwn(authenticatedActor, updateRequest)
              ).flatMap {
                case AuthUserCommands.UpdateUserSettingsResult.Forbidden =>
                  forbiddenResponse
                case AuthUserCommands.UpdateUserSettingsResult.InvalidCurrentPassword =>
                  invalidCurrentPasswordResponse
                case AuthUserCommands.UpdateUserSettingsResult.NotFound =>
                  NotFound(ErrorResponse("User not found.").asJson)
                case AuthUserCommands.UpdateUserSettingsResult.Updated(user) =>
                  Ok(userSettingsResponse(user).asJson)
              }
            yield response
          else
            SiteManagerUser.from(authenticatedActor) match
              case None =>
                forbiddenResponse
              case Some(siteManagerActor) =>
                for
                  updateRequest <- request.as[UpdateManagedUserSettingsRequest]
                  response <- AuthUserCommands.updateUserSettings(
                    databaseSession,
                    target,
                    AuthUserCommands.UpdateUserSettingsCommand.UpdateManaged(siteManagerActor, updateRequest)
                  ).flatMap {
                    case AuthUserCommands.UpdateUserSettingsResult.Forbidden =>
                      forbiddenResponse
                    case AuthUserCommands.UpdateUserSettingsResult.InvalidCurrentPassword =>
                      invalidCurrentPasswordResponse
                    case AuthUserCommands.UpdateUserSettingsResult.NotFound =>
                      NotFound(ErrorResponse("User not found.").asJson)
                    case AuthUserCommands.UpdateUserSettingsResult.Updated(user) =>
                      Ok(userSettingsResponse(user).asJson)
                  }
                yield response
        }

      case request @ POST -> Root / "api" / "auth" / "login" =>
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
                    Ok(
                      LoginResponse(
                        displayName = foundUser.displayName,
                        username = foundUser.username,
                        email = foundUser.email,
                        siteManager = foundUser.siteManager,
                        problemManager = foundUser.problemManager,
                        message = "Login successful"
                      ).asJson
                    ).map(_.addCookie(sessionCookie(sessionToken)))
                  )
                case false =>
                  invalidCredentialsResponse
              }
            case None => invalidCredentialsResponse
        yield response

      case request @ POST -> Root / "api" / "auth" / "register" =>
        for
          registerRequest <- request.as[RegisterRequest]
          _ <- logger.info(s"AuthRouter received register request for ${registerRequest.username.value}")
          response <- UsernameRules.validate(registerRequest.username) match
            case Some(validationError) =>
              BadRequest(ErrorResponse(validationError).asJson)
            case None =>
              databaseSession.withTransactionConnection { connection =>
                for
                  existingUser <- AuthUserTable.findByUsername(connection, registerRequest.username)
                  result <- existingUser match
                    case Some(_) =>
                      IO.pure(
                        Response[IO](status = Status.Conflict)
                          .withEntity(ErrorResponse("Username already exists, including case-only variations.").asJson)
                      )
                    case None =>
                      AuthUserTable
                        .insert(
                          connection,
                          username = registerRequest.username,
                          displayName = registerRequest.displayName,
                          email = registerRequest.email,
                          password = registerRequest.password
                        )
                        .flatMap { createdUser =>
                          sessionStore.createSession(createdUser.username).flatMap(sessionToken =>
                            Created(
                              LoginResponse(
                                displayName = createdUser.displayName,
                                username = createdUser.username,
                                email = createdUser.email,
                                siteManager = createdUser.siteManager,
                                problemManager = createdUser.problemManager,
                                message = "Registration successful"
                              ).asJson
                            ).map(_.addCookie(sessionCookie(sessionToken)))
                          )
                        }
                yield result
              }
        yield response
    }
