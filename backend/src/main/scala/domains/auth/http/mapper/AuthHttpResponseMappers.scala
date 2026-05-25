package domains.auth.http.mapper



import cats.effect.IO
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.model.response.{LoginResponse, RegisterResponse, SessionResponse}
import domains.auth.http.AuthHttpPlans
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.model.{AuthUser, SessionToken}
import shared.http.ApiMessages
import shared.http.utils.HttpResponseSupport.{errorResponse, successResponse, validationErrorResponse}
import domains.user.model.UserPreferences
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

  def sessionResponse(response: SessionResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def loggedOutResponse(output: AuthHttpPlans.LogoutOutput): IO[Response[IO]] =
    loggedOutResponse(output.clearedSessionCookie)

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
