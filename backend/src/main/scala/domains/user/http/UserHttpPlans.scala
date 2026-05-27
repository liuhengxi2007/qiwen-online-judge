package domains.user.http



import cats.effect.IO
import domains.auth.objects.{AuthUser, SiteManagerUser}
import domains.user.objects.Username
import shared.objects.{PageRequest, PageResponse}
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.objects.response.{UserAcceptedRanklistItem, UserListResponse, UserRanklistItem}
import domains.user.objects.request.{UpdateManagedUserPreferencesRequest, UpdateManagedUserProfileRequest, UpdateOwnPreferencesRequest, UpdateOwnProfileRequest, UserListRequest, UserSearchQuery}
import domains.user.objects.UserIdentity

import java.sql.Connection

object UserHttpPlans:

  case object ListUsers extends SiteManagerPlainUserHttpPlan[UserListRequest, UserListResponse]:

    override def execute(
      context: UserHttpContext,
      actor: SiteManagerUser,
      input: UserListRequest
    ): IO[UserListResponse] =
      UserQueryCommands.listUsers(context.databaseSession, actor, input)

  case object GetUserProfile extends AuthenticatedPlainUserHttpPlan[Username, UserQueryCommands.GetUserProfileResult]:

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: Username
    ): IO[UserQueryCommands.GetUserProfileResult] =
      UserQueryCommands.getUserProfile(context.databaseSession, actor, input)

  case object ListUserSuggestions extends AuthenticatedPlainUserHttpPlan[UserSearchQuery, List[UserIdentity]]:

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: UserSearchQuery
    ): IO[List[UserIdentity]] =
      UserQueryCommands.listSuggestions(context.databaseSession, actor, input)

  case object GetUserSettings extends AuthenticatedPlainUserHttpPlan[Username, UserMutationCommands.GetUserSettingsResult]:

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: Username
    ): IO[UserMutationCommands.GetUserSettingsResult] =
      UserMutationCommands.getUserSettings(context.databaseSession, actor, input)

  case object ListContributionRanklist extends AuthenticatedPlainUserHttpPlan[PageRequest, PageResponse[UserRanklistItem]]:

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[UserRanklistItem]] =
      UserQueryCommands.listContributionRanklist(context.databaseSession, actor, input)

  case object ListAcceptedRanklist extends AuthenticatedPlainUserHttpPlan[PageRequest, PageResponse[UserAcceptedRanklistItem]]:

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[UserAcceptedRanklistItem]] =
      UserQueryCommands.listAcceptedRanklist(context.databaseSession, actor, input)

  case object UpdateOwnProfile
      extends AuthenticatedTransactionUserHttpPlan[(Username, UpdateOwnProfileRequest), UserMutationCommands.UpdateUserSettingsResult]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnProfileRequest)
    ): IO[UserMutationCommands.UpdateUserSettingsResult] =
      val _ = context
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateOwnProfile(actor, request)
      UserMutationCommands.updateUserSettings(connection, targetUsername, command)

  case object UpdateOwnPreferences
      extends AuthenticatedTransactionUserHttpPlan[(Username, UpdateOwnPreferencesRequest), UserMutationCommands.UpdateUserSettingsResult]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnPreferencesRequest)
    ): IO[UserMutationCommands.UpdateUserSettingsResult] =
      val _ = context
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateOwnPreferences(actor, request)
      UserMutationCommands.updateUserSettings(connection, targetUsername, command)

  case object UpdateManagedProfile
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateManagedUserProfileRequest), UserMutationCommands.UpdateUserSettingsResult]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserProfileRequest)
    ): IO[UserMutationCommands.UpdateUserSettingsResult] =
      val _ = context
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateManagedProfile(actor, request)
      UserMutationCommands.updateUserSettings(connection, targetUsername, command)

  case object UpdateManagedPreferences
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateManagedUserPreferencesRequest), UserMutationCommands.UpdateUserSettingsResult]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserPreferencesRequest)
    ): IO[UserMutationCommands.UpdateUserSettingsResult] =
      val _ = context
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateManagedPreferences(actor, request)
      UserMutationCommands.updateUserSettings(connection, targetUsername, command)
