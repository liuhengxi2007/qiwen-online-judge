package domains.user.http

import cats.effect.IO
import domains.auth.model.{AuthUser, SiteManagerUser, Username}
import domains.shared.model.{PageRequest, PageResponse}
import domains.user.application.{UserMutationCommands, UserQueryCommands}
import domains.user.model.{AuthUserListItem, UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest, UserAcceptedRanklistItem, UserIdentity, UserRanklistItem}

import java.sql.Connection

object UserHttpPlans:

  final case class UpdateUserSettingsOutput(
    result: UserMutationCommands.UpdateUserSettingsResult,
    clearSessionCookie: Boolean
  )

  case object ListUsers extends SiteManagerPlainUserHttpPlan[Unit, List[AuthUserListItem]]:

    override val name: String = "ListUsers"

    override def execute(
      context: UserHttpContext,
      actor: SiteManagerUser,
      input: Unit
    ): IO[List[AuthUserListItem]] =
      val _ = input
      UserMutationCommands.listUsers(context.databaseSession, actor)

  case object GetUserProfile extends AuthenticatedPlainUserHttpPlan[Username, UserQueryCommands.GetUserProfileResult]:

    override val name: String = "GetUserProfile"

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: Username
    ): IO[UserQueryCommands.GetUserProfileResult] =
      UserQueryCommands.getUserProfile(context.databaseSession, actor, input)

  case object ListUserSuggestions extends AuthenticatedPlainUserHttpPlan[String, List[UserIdentity]]:

    override val name: String = "ListUserSuggestions"

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: String
    ): IO[List[UserIdentity]] =
      UserQueryCommands.listSuggestions(context.databaseSession, actor, input)

  case object GetUserSettings extends AuthenticatedPlainUserHttpPlan[Username, UserMutationCommands.GetUserSettingsResult]:

    override val name: String = "GetUserSettings"

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: Username
    ): IO[UserMutationCommands.GetUserSettingsResult] =
      UserMutationCommands.getUserSettings(context.databaseSession, actor, input)

  case object ListContributionRanklist extends AuthenticatedPlainUserHttpPlan[PageRequest, PageResponse[UserRanklistItem]]:

    override val name: String = "ListContributionRanklist"

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[UserRanklistItem]] =
      UserQueryCommands.listContributionRanklist(context.databaseSession, actor, input)

  case object ListAcceptedRanklist extends AuthenticatedPlainUserHttpPlan[PageRequest, PageResponse[UserAcceptedRanklistItem]]:

    override val name: String = "ListAcceptedRanklist"

    override def execute(
      context: UserHttpContext,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[UserAcceptedRanklistItem]] =
      UserQueryCommands.listAcceptedRanklist(context.databaseSession, actor, input)

  case object UpdateUserPermissions
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateUserPermissionsRequest), UserMutationCommands.UpdateUserPermissionsResult]:

    override val name: String = "UpdateUserPermissions"

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateUserPermissionsRequest)
    ): IO[UserMutationCommands.UpdateUserPermissionsResult] =
      val _ = context
      val (targetUsername, request) = input
      UserMutationCommands.updateUserPermissions(connection, actor.authUser, targetUsername, request)

  case object UpdateOwnSettings
      extends AuthenticatedTransactionUserHttpPlan[(Username, UpdateOwnSettingsRequest), UpdateUserSettingsOutput]:

    override val name: String = "UpdateOwnSettings"

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnSettingsRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateOwn(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = passwordChangedByActor(actor, targetUsername, result)
        )

  case object UpdateManagedSettings
      extends SiteManagerTransactionUserHttpPlan[(Username, UpdateManagedUserSettingsRequest), UpdateUserSettingsOutput]:

    override val name: String = "UpdateManagedSettings"

    override def execute(
      context: UserHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserSettingsRequest)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, request) = input
      val command = UserMutationCommands.UpdateUserSettingsCommand.UpdateManaged(actor, request)
      for
        result <- UserMutationCommands.updateUserSettings(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateUserSettingsOutput(
          result = result,
          clearSessionCookie = false
        )

  case object DeleteUser extends SiteManagerTransactionUserHttpPlan[Username, UserMutationCommands.DeleteUserResult]:

    override val name: String = "DeleteUser"

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
