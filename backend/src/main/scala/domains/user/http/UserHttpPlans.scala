package domains.user.http



import cats.effect.IO
import domains.auth.model.{AuthUser, SiteManagerUser}
import domains.user.model.Username
import shared.model.{PageRequest, PageResponse}
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.model.response.{UserAcceptedRanklistItem, UserListResponse, UserRanklistItem}
import domains.user.model.request.{UpdateManagedUserAccountRequest, UpdateManagedUserPreferencesRequest, UpdateManagedUserProfileRequest, UpdateOwnAccountRequest, UpdateOwnPreferencesRequest, UpdateOwnProfileRequest, UpdateUserPermissionsRequest, UserListRequest, UserSearchQuery}
import domains.user.model.UserIdentity

import java.sql.Connection

object UserHttpPlans:

  final case class UpdateUserSettingsOutput(
    result: UserMutationCommands.UpdateUserSettingsResult,
    clearSessionCookie: Boolean
  )

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

  case object UpdateUserPermissions
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateUserPermissionsRequest), UserMutationCommands.UpdateUserPermissionsResult]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateUserPermissionsRequest)
    ): IO[UserMutationCommands.UpdateUserPermissionsResult] =
      val _ = context
      val (targetUsername, request) = input
      UserMutationCommands.updateUserPermissions(connection, actor.authUser, targetUsername, request)

  case object UpdateOwnProfile
      extends AuthenticatedTransactionUserHttpPlan[(Username, UpdateOwnProfileRequest), UpdateUserSettingsOutput]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnProfileRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateOwnProfile(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = passwordChangedByActor(actor, targetUsername, result)
        )

  case object UpdateOwnPreferences
      extends AuthenticatedTransactionUserHttpPlan[(Username, UpdateOwnPreferencesRequest), UpdateUserSettingsOutput]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnPreferencesRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateOwnPreferences(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = passwordChangedByActor(actor, targetUsername, result)
        )

  case object UpdateOwnAccount
      extends AuthenticatedTransactionUserHttpPlan[(Username, UpdateOwnAccountRequest), UpdateUserSettingsOutput]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnAccountRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateOwnAccount(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = passwordChangedByActor(actor, targetUsername, result)
        )

  case object UpdateManagedProfile
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateManagedUserProfileRequest), UpdateUserSettingsOutput]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserProfileRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateManagedProfile(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = false
        )

  case object UpdateManagedPreferences
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateManagedUserPreferencesRequest), UpdateUserSettingsOutput]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserPreferencesRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateManagedPreferences(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = false
        )

  case object UpdateManagedAccount
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateManagedUserAccountRequest), UpdateUserSettingsOutput]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserAccountRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateManagedAccount(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = false
        )

  case object DeleteUser extends SiteManagerTransactionUserHttpPlan[Username, UserMutationCommands.DeleteUserResult]:

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: Username
    ): IO[UserMutationCommands.DeleteUserResult] =
      val _ = context
      UserMutationCommands.deleteUser(connection, actor.authUser, input)

  private def revokePasswordChangedSessions(
    context: UserHttpContext,
    targetUsername: Username,
    result: UserMutationCommands.UpdateUserSettingsResult
  ): IO[Unit] =
    result match
      case UserMutationCommands.UpdateUserSettingsResult.Updated(_, true) =>
        context.sessionStore.deleteSessionsForUsername(targetUsername)
      case _ =>
        IO.unit

  private def passwordChangedByActor(
    actor: AuthUser,
    targetUsername: Username,
    result: UserMutationCommands.UpdateUserSettingsResult
  ): Boolean =
    result match
      case UserMutationCommands.UpdateUserSettingsResult.Updated(_, true) =>
        actor.username.value == targetUsername.value
      case _ =>
        false
