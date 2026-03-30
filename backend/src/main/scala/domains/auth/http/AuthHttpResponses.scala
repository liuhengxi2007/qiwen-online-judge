package domains.auth.http

import cats.effect.IO
import domains.auth.application.AuthUserCommands
import domains.auth.model.{AuthUser, AuthUserListItem, LoginResponse, SessionResponse}
import domains.shared.model.ErrorResponse
import io.circe.syntax.*
import org.http4s.{Response, ResponseCookie, SameSite, Status}
import org.http4s.dsl.io.Http4sDsl

object AuthHttpResponses:

  private val sessionCookieName = "qiwen_session"

  def invalidCredentialsResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, "Invalid username or password.")

  def invalidCurrentPasswordResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, "Current password is incorrect.")

  def unauthorizedResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, "Authentication required.")

  def forbiddenResponse: IO[Response[IO]] =
    errorResponse(Status.Forbidden, "Site manager permission required.")

  def protectedAdminResponse: IO[Response[IO]] =
    errorResponse(Status.Forbidden, "The admin account permissions cannot be modified.")

  def userNotFoundResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, "User not found.")

  def usernameConflictResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, "Username already exists, including case-only variations.")

  def validationErrorResponse(message: String): IO[Response[IO]] =
    errorResponse(Status.BadRequest, message)

  def loggedOutResponse(clearedSessionCookie: ResponseCookie)(using dsl: Http4sDsl[IO]): IO[Response[IO]] =
    import dsl.*
    Ok(ErrorResponse("Logged out.").asJson).map(_.addCookie(clearedSessionCookie))

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

  def toSessionResponse(user: AuthUser): SessionResponse =
    SessionResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )

  def toUserListItem(user: AuthUser): AuthUserListItem =
    AuthUserListItem(
      username = user.username,
      displayName = user.displayName,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )

  def toLoginResponse(user: AuthUser, message: String): LoginResponse =
    LoginResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )

  def mapGetUserSettingsResult(result: AuthUserCommands.GetUserSettingsResult)(using dsl: Http4sDsl[IO]): IO[Response[IO]] =
    import dsl.*
    result match
      case AuthUserCommands.GetUserSettingsResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.GetUserSettingsResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.GetUserSettingsResult.Found(targetUser) =>
        Ok(toSessionResponse(targetUser).asJson)

  def mapUpdateUserPermissionsResult(result: AuthUserCommands.UpdateUserPermissionsResult)(using dsl: Http4sDsl[IO]): IO[Response[IO]] =
    import dsl.*
    result match
      case AuthUserCommands.UpdateUserPermissionsResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.UpdateUserPermissionsResult.ProtectedAdmin =>
        protectedAdminResponse
      case AuthUserCommands.UpdateUserPermissionsResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.UpdateUserPermissionsResult.Updated(user) =>
        Ok(toUserListItem(user).asJson)

  def mapUpdateUserSettingsResult(result: AuthUserCommands.UpdateUserSettingsResult)(using dsl: Http4sDsl[IO]): IO[Response[IO]] =
    import dsl.*
    result match
      case AuthUserCommands.UpdateUserSettingsResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.UpdateUserSettingsResult.InvalidCurrentPassword =>
        invalidCurrentPasswordResponse
      case AuthUserCommands.UpdateUserSettingsResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.UpdateUserSettingsResult.Updated(user) =>
        Ok(toSessionResponse(user).asJson)

  private def errorResponse(status: Status, message: String): IO[Response[IO]] =
    IO.pure(
      Response[IO](status = status)
        .withEntity(ErrorResponse(message).asJson)
    )
