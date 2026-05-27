package domains.user.http.mapper



import cats.effect.IO
import domains.auth.objects.AuthUser
import domains.user.objects.UserPreferences
import shared.objects.PageResponse
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.objects.response.{UserAcceptedRanklistItem, UserListResponse, UserRanklistItem, UserSettingsResponse}
import domains.user.objects.UserIdentity
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, Status}
import shared.http.ApiMessages
import shared.http.utils.HttpResponseSupport

object UserHttpResponseMappers:

  def userNotFoundResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.NotFound, ApiMessages.userNotFound)

  def forbiddenResponse: IO[Response[IO]] =
    HttpResponseSupport.errorResponse(Status.Forbidden, ApiMessages.siteManagerRequired)

  def validationErrorResponse(message: String): IO[Response[IO]] =
    HttpResponseSupport.validationErrorResponse(message)

  def toUserSettingsResponse(user: AuthUser): UserSettingsResponse =
    UserSettingsResponse(
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
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserSettingsResponse(targetUser).asJson))

  def mapUpdateUserSettingsResult(result: UserMutationCommands.UpdateUserSettingsResult): IO[Response[IO]] =
    result match
      case UserMutationCommands.UpdateUserSettingsResult.Forbidden =>
        forbiddenResponse
      case UserMutationCommands.UpdateUserSettingsResult.NotFound =>
        userNotFoundResponse
      case UserMutationCommands.UpdateUserSettingsResult.Updated(user) =>
        IO.pure(Response[IO](status = Status.Ok).withEntity(toUserSettingsResponse(user).asJson))
