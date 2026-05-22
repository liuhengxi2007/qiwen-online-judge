package domains.user.http.response



import cats.effect.IO
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.http.response.AuthHttpResponses
import domains.auth.model.AuthUser
import domains.auth.application.output.{SessionResponse}
import shared.model.PageResponse
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.http.UserHttpPlans.UpdateUserSettingsOutput
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.application.output.{AuthUserListItem, UserAcceptedRanklistItem, UserListResponse, UserRanklistItem}
import domains.user.model.UserIdentity
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, Status}

object UserHttpResponses:

  def userNotFoundResponse: IO[Response[IO]] =
    AuthHttpResponses.userNotFoundResponse

  def forbiddenResponse: IO[Response[IO]] =
    AuthHttpResponses.forbiddenResponse

  def protectedAdminResponse: IO[Response[IO]] =
    AuthHttpResponses.protectedAdminResponse

  def protectedAdminDeletionResponse: IO[Response[IO]] =
    AuthHttpResponses.protectedAdminDeletionResponse

  def selfDeletionResponse: IO[Response[IO]] =
    AuthHttpResponses.selfDeletionResponse

  def invalidCurrentPasswordResponse: IO[Response[IO]] =
    AuthHttpResponses.invalidCurrentPasswordResponse

  def userOwnsResourcesResponse: IO[Response[IO]] =
    AuthHttpResponses.userOwnsResourcesResponse

  def validationErrorResponse(message: String): IO[Response[IO]] =
    AuthHttpResponses.validationErrorResponse(message)

  def toSessionResponse(user: AuthUser): SessionResponse =
    AuthHttpResponses.toSessionResponse(user)

  def toUserListItem(user: AuthUser): AuthUserListItem =
    AuthHttpResponses.toUserListItem(user)

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
      if output.clearSessionCookie then response.addCookie(AuthHttpResponses.clearedSessionCookie)
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
