package domains.auth.http

import cats.effect.IO
import domains.auth.application.AuthUserCommands
import domains.auth.model.{AuthUser, AuthUserListItem, LoginResponse, RegisterResponse, SessionResponse, UserAcceptedRanklistItem, UserPreferences, UserRanklistItem}
import domains.judger.model.RegisteredJudgerListItem
import domains.shared.http.HttpResponseSupport.{errorResponse, validationErrorResponse}
import domains.shared.model.{ErrorResponse, PageResponse, SuccessResponse}
import io.circe.syntax.*
import org.http4s.{Response, ResponseCookie, SameSite, Status}
import org.http4s.circe.CirceEntityEncoder.*

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

  def loggedOutResponse(clearedSessionCookie: ResponseCookie): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(ErrorResponse("Logged out.").asJson).addCookie(clearedSessionCookie))

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
      preferences =
        UserPreferences(
          displayMode = user.displayMode,
          locale = user.locale,
          problemTitleDisplayMode = user.problemTitleDisplayMode
        ),
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
      preferences =
        UserPreferences(
          displayMode = user.displayMode,
          locale = user.locale,
          problemTitleDisplayMode = user.problemTitleDisplayMode
        ),
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )

  def toRegisterResponse(user: AuthUser, message: String): RegisterResponse =
    RegisterResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences =
        UserPreferences(
          displayMode = user.displayMode,
          locale = user.locale,
          problemTitleDisplayMode = user.problemTitleDisplayMode
        ),
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )

  def sessionResponse(response: SessionResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def listUsersResponse(users: List[AuthUserListItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(users.asJson))

  def listJudgersResponse(judgers: List[RegisteredJudgerListItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(judgers.asJson))

  def listContributionRanklistResponse(response: PageResponse[UserRanklistItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def listAcceptedRanklistResponse(response: PageResponse[UserAcceptedRanklistItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def loggedOutResponse(output: AuthHttpPlans.LogoutOutput): IO[Response[IO]] =
    loggedOutResponse(output.clearedSessionCookie)

  def loginResponse(output: AuthHttpPlans.LoginOutput): IO[Response[IO]] =
    output match
      case AuthHttpPlans.LoginOutput.InvalidCredentials =>
        invalidCredentialsResponse
      case AuthHttpPlans.LoginOutput.LoggedIn(user, sessionToken) =>
        IO.pure(
          Response[IO](status = Status.Ok)
            .withEntity(toLoginResponse(user, "Login successful").asJson)
            .addCookie(sessionCookie(sessionToken))
        )

  def registerResponse(output: AuthHttpPlans.RegisterOutput): IO[Response[IO]] =
    output match
      case AuthHttpPlans.RegisterOutput.ValidationFailed(message) =>
        validationErrorResponse(message)
      case AuthHttpPlans.RegisterOutput.UsernameConflict =>
        usernameConflictResponse
      case AuthHttpPlans.RegisterOutput.UsernameConflictsWithUserGroup =>
        usernameConflictsWithUserGroupResponse
      case AuthHttpPlans.RegisterOutput.Registered(user, sessionToken) =>
        IO.pure(
          Response[IO](status = Status.Created)
            .withEntity(toRegisterResponse(user, "Registration successful").asJson)
            .addCookie(sessionCookie(sessionToken))
        )

  def updateUserSettingsResponse(output: AuthHttpPlans.UpdateUserSettingsOutput): IO[Response[IO]] =
    output match
      case AuthHttpPlans.UpdateUserSettingsOutput.ValidationFailed(message) =>
        validationErrorResponse(message)
      case AuthHttpPlans.UpdateUserSettingsOutput.Completed(result, clearCurrentSessionCookie) =>
        mapUpdateUserSettingsResult(result).map { response =>
          if clearCurrentSessionCookie then response.addCookie(clearedSessionCookie)
          else response
        }

  def mapGetUserSettingsResult(result: AuthUserCommands.GetUserSettingsResult): IO[Response[IO]] =
    result match
      case AuthUserCommands.GetUserSettingsResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.GetUserSettingsResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.GetUserSettingsResult.Found(targetUser) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toSessionResponse(targetUser).asJson))

  def mapGetUserProfileResult(result: AuthUserCommands.GetUserProfileResult): IO[Response[IO]] =
    result match
      case AuthUserCommands.GetUserProfileResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.GetUserProfileResult.Found(profile) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(profile.asJson))

  def mapUpdateUserPermissionsResult(result: AuthUserCommands.UpdateUserPermissionsResult): IO[Response[IO]] =
    result match
      case AuthUserCommands.UpdateUserPermissionsResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.UpdateUserPermissionsResult.ProtectedAdmin =>
        protectedAdminResponse
      case AuthUserCommands.UpdateUserPermissionsResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.UpdateUserPermissionsResult.Updated(user) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserListItem(user).asJson))

  def mapUpdateUserSettingsResult(result: AuthUserCommands.UpdateUserSettingsResult): IO[Response[IO]] =
    result match
      case AuthUserCommands.UpdateUserSettingsResult.Forbidden =>
        forbiddenResponse
      case AuthUserCommands.UpdateUserSettingsResult.InvalidCurrentPassword =>
        invalidCurrentPasswordResponse
      case AuthUserCommands.UpdateUserSettingsResult.NotFound =>
        userNotFoundResponse
      case AuthUserCommands.UpdateUserSettingsResult.Updated(user, _) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toSessionResponse(user).asJson))

  def mapDeleteUserResult(result: AuthUserCommands.DeleteUserResult): IO[Response[IO]] =
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
        IO.pure(Response[IO](status = Status.Ok).withEntity(SuccessResponse("User deleted.").asJson))
