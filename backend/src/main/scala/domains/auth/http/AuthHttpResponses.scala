package domains.auth.http

import cats.effect.IO
import domains.auth.application.AuthUserCommands
import domains.auth.model.{AuthUser, AuthUserListItem, LoginResponse, RegisterResponse, SessionResponse}
import domains.shared.http.HttpResponseSupport.{errorResponse, validationErrorResponse}
import domains.shared.model.{ErrorResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, ResponseCookie, SameSite, Status}
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl

object AuthHttpResponses:

  private val sessionCookieName = "qiwen_session"

  def validationErrorResponse(message: String): IO[Response[IO]] =
    domains.shared.http.HttpResponseSupport.validationErrorResponse(message)

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

  def protectedAdminDeletionResponse: IO[Response[IO]] =
    errorResponse(Status.Forbidden, "The admin account cannot be deleted.")

  def selfDeletionResponse: IO[Response[IO]] =
    errorResponse(Status.BadRequest, "You cannot delete your own account.")

  def userNotFoundResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, "User not found.")

  def userOwnsResourcesResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, "User owns existing resources and cannot be deleted.")

  def usernameConflictResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, "Username already exists, including case-only variations.")

  def usernameConflictsWithUserGroupResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, "Username conflicts with an existing user group slug.")

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

  def toRegisterResponse(user: AuthUser, message: String): RegisterResponse =
    RegisterResponse(
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
      case AuthUserCommands.UpdateUserSettingsResult.Updated(user, _) =>
        Ok(toSessionResponse(user).asJson)

  def mapDeleteUserResult(result: AuthUserCommands.DeleteUserResult)(using dsl: Http4sDsl[IO]): IO[Response[IO]] =
    import dsl.*
    result match
      case AuthUserCommands.DeleteUserResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.DeleteUserResult.ProtectedAdmin =>
        protectedAdminDeletionResponse
      case AuthUserCommands.DeleteUserResult.CannotDeleteSelf =>
        selfDeletionResponse
      case AuthUserCommands.DeleteUserResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.DeleteUserResult.HasOwnedResources =>
        userOwnsResourcesResponse
      case AuthUserCommands.DeleteUserResult.Deleted =>
        Ok(SuccessResponse("User deleted.").asJson)
