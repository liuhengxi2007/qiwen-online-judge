package domains.auth.http.mapper



import cats.effect.IO
import domains.auth.application.AuthCommands
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.objects.response.{AuthAccountListItem, LoginResponse, RegisterResponse, SessionResponse}
import domains.auth.http.AuthHttpPlans
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.objects.{AuthUser, SessionToken}
import shared.http.ApiMessages
import shared.http.utils.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import domains.user.objects.UserPreferences
import io.circe.syntax.*
import org.http4s.{Response, ResponseCookie, SameSite, Status}
import org.http4s.circe.CirceEntityEncoder.*

object AuthHttpResponseMappers:

  private val sessionCookieName = "qiwen_session"

  def validationErrorResponse(message: String): IO[Response[IO]] =
    shared.http.utils.HttpResponseSupport.validationErrorResponse(message)

  def invalidCredentialsResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, ApiMessages.invalidCredentials)

  def invalidCurrentPasswordResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, ApiMessages.invalidCurrentPassword)

  def unauthorizedResponse: IO[Response[IO]] =
    errorResponse(Status.Unauthorized, ApiMessages.authenticationRequired)

  def forbiddenResponse: IO[Response[IO]] =
    errorResponse(Status.Forbidden, ApiMessages.siteManagerRequired)

  def protectedAdminResponse: IO[Response[IO]] =
    errorResponse(Status.Forbidden, ApiMessages.adminPermissionsImmutable)

  def protectedAdminDeletionResponse: IO[Response[IO]] =
    errorResponse(Status.Forbidden, ApiMessages.adminDeleteForbidden)

  def selfDeletionResponse: IO[Response[IO]] =
    errorResponse(Status.BadRequest, ApiMessages.cannotDeleteSelf)

  def userNotFoundResponse: IO[Response[IO]] =
    errorResponse(Status.NotFound, ApiMessages.userNotFound)

  def userOwnsResourcesResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, ApiMessages.userHasOwnedResources)

  def usernameConflictResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, ApiMessages.usernameExists)

  def usernameConflictsWithUserGroupResponse: IO[Response[IO]] =
    errorResponse(Status.Conflict, ApiMessages.usernameConflictsWithGroup)

  def loggedOutResponse(clearedSessionCookie: ResponseCookie): IO[Response[IO]] =
    successResponse(Status.Ok, ApiMessages.loggedOut).map(_.addCookie(clearedSessionCookie))

  def sessionCookie(token: SessionToken): ResponseCookie =
    ResponseCookie(
      name = sessionCookieName,
      content = token.value,
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
          problemTitleDisplayMode = user.problemTitleDisplayMode,
          autoMarkMessageRead = user.autoMarkMessageRead
        ),
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
          problemTitleDisplayMode = user.problemTitleDisplayMode,
          autoMarkMessageRead = user.autoMarkMessageRead
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
          problemTitleDisplayMode = user.problemTitleDisplayMode,
          autoMarkMessageRead = user.autoMarkMessageRead
        ),
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )

  def toAuthAccountListItem(user: AuthUser): AuthAccountListItem =
    AuthAccountListItem(
      username = user.username,
      displayName = user.displayName,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )

  def sessionResponse(response: SessionResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def loggedOutResponse(output: AuthHttpPlans.LogoutOutput): IO[Response[IO]] =
    loggedOutResponse(output.clearedSessionCookie)

  def mapUpdateUserPermissionsResult(result: AuthCommands.UpdateUserPermissionsResult): IO[Response[IO]] =
    result match
      case AuthCommands.UpdateUserPermissionsResult.Forbidden =>
        forbiddenResponse
      case AuthCommands.UpdateUserPermissionsResult.ProtectedAdmin =>
        protectedAdminResponse
      case AuthCommands.UpdateUserPermissionsResult.NotFound =>
        userNotFoundResponse
      case AuthCommands.UpdateUserPermissionsResult.Updated(user) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toAuthAccountListItem(user).asJson))

  def mapUpdateAccountResult(result: AuthCommands.UpdateAccountResult): IO[Response[IO]] =
    result match
      case AuthCommands.UpdateAccountResult.Forbidden =>
        forbiddenResponse
      case AuthCommands.UpdateAccountResult.InvalidCurrentPassword =>
        invalidCurrentPasswordResponse
      case AuthCommands.UpdateAccountResult.NotFound =>
        userNotFoundResponse
      case AuthCommands.UpdateAccountResult.Updated(user, _) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toSessionResponse(user).asJson))

  def mapUpdateAccountOutput(output: AuthHttpPlans.UpdateAccountOutput): IO[Response[IO]] =
    mapUpdateAccountResult(output.result).map { response =>
      if output.clearSessionCookie then response.addCookie(clearedSessionCookie)
      else response
    }

  def mapDeleteAccountResult(result: AuthCommands.DeleteAccountResult): IO[Response[IO]] =
    result match
      case AuthCommands.DeleteAccountResult.Forbidden =>
        forbiddenResponse
      case AuthCommands.DeleteAccountResult.ProtectedAdmin =>
        protectedAdminDeletionResponse
      case AuthCommands.DeleteAccountResult.CannotDeleteSelf =>
        selfDeletionResponse
      case AuthCommands.DeleteAccountResult.NotFound =>
        userNotFoundResponse
      case AuthCommands.DeleteAccountResult.HasOwnedResources =>
        userOwnsResourcesResponse
      case AuthCommands.DeleteAccountResult.Deleted =>
        successResponse(Status.Ok, ApiMessages.userDeleted)

  def loginResponse(output: LoginResult): IO[Response[IO]] =
    output match
      case LoginResult.InvalidCredentials =>
        invalidCredentialsResponse
      case LoginResult.LoggedIn(user, sessionToken) =>
        IO.pure(
          Response[IO](status = Status.Ok)
            .withEntity(toLoginResponse(user, "Login successful").asJson)
            .addCookie(sessionCookie(sessionToken))
        )

  def registerResponse(output: RegisterResult): IO[Response[IO]] =
    output match
      case RegisterResult.ValidationFailed(message) =>
        validationErrorResponse(message)
      case RegisterResult.UsernameConflict =>
        usernameConflictResponse
      case RegisterResult.UsernameConflictsWithUserGroup =>
        usernameConflictsWithUserGroupResponse
      case RegisterResult.Registered(user, sessionToken) =>
        IO.pure(
          Response[IO](status = Status.Created)
            .withEntity(toRegisterResponse(user, "Registration successful").asJson)
            .addCookie(sessionCookie(sessionToken))
        )
