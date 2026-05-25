package domains.user.http.mapper



import cats.effect.IO
import domains.auth.model.AuthUser
import domains.auth.model.response.{SessionResponse}
import domains.user.model.UserPreferences
import shared.model.PageResponse
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.http.UserHttpPlans.UpdateUserSettingsOutput
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.model.response.{AuthUserListItem, UserAcceptedRanklistItem, UserListResponse, UserRanklistItem}
import domains.user.model.UserIdentity
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, ResponseCookie, SameSite, Status}
import shared.http.ApiMessages
import shared.http.utils.HttpResponseSupport

object UserHttpResponseMappers:

  private val sessionCookieName = "qiwen_session"

  private val clearedSessionCookie: ResponseCookie =
    ResponseCookie(
      name = sessionCookieName,
      content = "",
      path = Some("/"),
      httpOnly = true,
      sameSite = Some(SameSite.Lax),
      maxAge = Some(0L)
    )

  def userNotFoundResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.NotFound, ApiMessages.userNotFound)

  def forbiddenResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.Forbidden, ApiMessages.siteManagerRequired)

  def protectedAdminResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.Forbidden, ApiMessages.adminPermissionsImmutable)

  def protectedAdminDeletionResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.Forbidden, ApiMessages.adminDeleteForbidden)

  def selfDeletionResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.BadRequest, ApiMessages.cannotDeleteSelf)

  def invalidCurrentPasswordResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.Unauthorized, ApiMessages.invalidCurrentPassword)

  def userOwnsResourcesResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.Conflict, ApiMessages.userHasOwnedResources)

  def validationErrorResponse(message: String): IO[Response[IO]] =
    HttpResponseSupport.validationErrorResponse(message)

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

  def toUserListItem(user: AuthUser): AuthUserListItem =
    AuthUserListItem(
      username = user.username,
      displayName = user.displayName,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )

  def listUsersResponse(users: UserListResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(users.asJson))

  def listUserSuggestionsResponse(users: List[UserIdentity]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(users.asJson))

  def listContributionRanklistResponse(response: PageResponse[UserRanklistItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def listAcceptedRanklistResponse(response: PageResponse[UserAcceptedRanklistItem]): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(response.asJson))

  def mapGetUserProfileResult(result: UserQueryCommands.GetUserProfileResult): IO[Response[IO]] =
    result match
      case UserQueryCommands.GetUserProfileResult.NotFound =>
        userNotFoundResponse
      case UserQueryCommands.GetUserProfileResult.Found(profile) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(profile.asJson))

  def mapGetUserSettingsResult(result: UserMutationCommands.GetUserSettingsResult): IO[Response[IO]] =
    result match
      case UserMutationCommands.GetUserSettingsResult.Forbidden =>
        forbiddenResponse
      case UserMutationCommands.GetUserSettingsResult.NotFound =>
        userNotFoundResponse
      case UserMutationCommands.GetUserSettingsResult.Found(targetUser) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toSessionResponse(targetUser).asJson))

  def mapUpdateUserPermissionsResult(result: UserMutationCommands.UpdateUserPermissionsResult): IO[Response[IO]] =
    result match
      case UserMutationCommands.UpdateUserPermissionsResult.Forbidden =>
        forbiddenResponse
      case UserMutationCommands.UpdateUserPermissionsResult.ProtectedAdmin =>
        protectedAdminResponse
      case UserMutationCommands.UpdateUserPermissionsResult.NotFound =>
        userNotFoundResponse
      case UserMutationCommands.UpdateUserPermissionsResult.Updated(user) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserListItem(user).asJson))

  def mapUpdateUserSettingsResult(result: UserMutationCommands.UpdateUserSettingsResult): IO[Response[IO]] =
    result match
      case UserMutationCommands.UpdateUserSettingsResult.Forbidden =>
        forbiddenResponse
      case UserMutationCommands.UpdateUserSettingsResult.InvalidCurrentPassword =>
        invalidCurrentPasswordResponse
      case UserMutationCommands.UpdateUserSettingsResult.NotFound =>
        userNotFoundResponse
      case UserMutationCommands.UpdateUserSettingsResult.Updated(user, _) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toSessionResponse(user).asJson))

  def mapUpdateUserSettingsOutput(output: UpdateUserSettingsOutput): IO[Response[IO]] =
    mapUpdateUserSettingsResult(output.result).map { response =>
      if output.clearSessionCookie then response.addCookie(clearedSessionCookie)
      else response
    }

  def mapDeleteUserResult(result: UserMutationCommands.DeleteUserResult): IO[Response[IO]] =
    result match
      case UserMutationCommands.DeleteUserResult.Forbidden =>
        forbiddenResponse
      case UserMutationCommands.DeleteUserResult.ProtectedAdmin =>
        protectedAdminDeletionResponse
      case UserMutationCommands.DeleteUserResult.CannotDeleteSelf =>
        selfDeletionResponse
      case UserMutationCommands.DeleteUserResult.NotFound =>
        userNotFoundResponse
      case UserMutationCommands.DeleteUserResult.HasOwnedResources =>
        userOwnsResourcesResponse
      case UserMutationCommands.DeleteUserResult.Deleted =>
        shared.http.utils.HttpResponseSupport.successResponse(Status.Ok, shared.http.ApiMessages.userDeleted)
