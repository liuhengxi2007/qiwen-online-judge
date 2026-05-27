package domains.user.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.{AuthUser, SiteManagerUser}
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.Username
import domains.user.objects.request.{UpdateManagedUserPreferencesRequest, UpdateManagedUserProfileRequest, UpdateOwnPreferencesRequest, UpdateOwnProfileRequest}
import domains.user.objects.{UserDisplayMode, UserLocale}
import domains.user.table.user.UserTable

import java.sql.Connection

object UserMutationCommands:

  enum GetUserSettingsResult:
    case Forbidden
    case NotFound
    case Found(user: AuthUser)

  enum UpdateUserSettingsCommand:
    case UpdateOwnProfile(actor: AuthUser, request: UpdateOwnProfileRequest)
    case UpdateOwnPreferences(actor: AuthUser, request: UpdateOwnPreferencesRequest)
    case UpdateManagedProfile(actor: SiteManagerUser, request: UpdateManagedUserProfileRequest)
    case UpdateManagedPreferences(actor: SiteManagerUser, request: UpdateManagedUserPreferencesRequest)

  enum UpdateUserSettingsResult:
    case Forbidden
    case NotFound
    case Updated(user: AuthUser)

  def getUserSettings(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    targetUsername: Username
  ): IO[GetUserSettingsResult] =
    if !canReadUserSettings(actor, targetUsername) then
      IO.pure(GetUserSettingsResult.Forbidden)
    else
      databaseSession.withTransactionConnection(connection =>
        UserTable.findByUsername(connection, targetUsername)
      ).map {
        case Some(targetUser) => GetUserSettingsResult.Found(targetUser)
        case None => GetUserSettingsResult.NotFound
      }

  def updateUserSettings(
    connection: Connection,
    targetUsername: Username,
    command: UpdateUserSettingsCommand
  ): IO[UpdateUserSettingsResult] =
    if !commandCanAccessTarget(command, targetUsername) then
      IO.pure(UpdateUserSettingsResult.Forbidden)
    else
      UserTable.findByUsername(connection, targetUsername).flatMap {
        case None =>
          IO.pure(UpdateUserSettingsResult.NotFound)
        case Some(targetUser) =>
          command match
            case UpdateUserSettingsCommand.UpdateOwnProfile(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = request.displayName,
                displayMode = targetUser.displayMode,
                locale = targetUser.locale,
                problemTitleDisplayMode = targetUser.problemTitleDisplayMode,
                autoMarkMessageRead = targetUser.autoMarkMessageRead
              )
            case UpdateUserSettingsCommand.UpdateOwnPreferences(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = targetUser.displayName,
                displayMode = request.preferences.displayMode,
                locale = request.preferences.locale,
                problemTitleDisplayMode = request.preferences.problemTitleDisplayMode,
                autoMarkMessageRead = request.preferences.autoMarkMessageRead
              )
            case UpdateUserSettingsCommand.UpdateManagedProfile(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = request.displayName,
                displayMode = targetUser.displayMode,
                locale = targetUser.locale,
                problemTitleDisplayMode = targetUser.problemTitleDisplayMode,
                autoMarkMessageRead = targetUser.autoMarkMessageRead
              )
            case UpdateUserSettingsCommand.UpdateManagedPreferences(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = targetUser.displayName,
                displayMode = request.preferences.displayMode,
                locale = request.preferences.locale,
                problemTitleDisplayMode = request.preferences.problemTitleDisplayMode,
                autoMarkMessageRead = request.preferences.autoMarkMessageRead
              )
      }

  private def updateSettingsRecord(
    connection: Connection,
    targetUser: AuthUser,
    displayName: domains.user.objects.DisplayName,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean
  ): IO[UpdateUserSettingsResult] =
    UserTable.updateSettings(
      connection,
      targetUser.username,
      displayName = displayName,
      displayMode = displayMode,
      locale = locale,
      problemTitleDisplayMode = problemTitleDisplayMode,
      autoMarkMessageRead = autoMarkMessageRead
    ).map {
      case Some(user) => UpdateUserSettingsResult.Updated(user)
      case None => UpdateUserSettingsResult.NotFound
    }

  private def commandCanAccessTarget(command: UpdateUserSettingsCommand, targetUsername: Username): Boolean =
    command match
      case UpdateUserSettingsCommand.UpdateOwnProfile(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateOwnPreferences(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateManagedProfile(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value
      case UpdateUserSettingsCommand.UpdateManagedPreferences(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value

  private def canReadUserSettings(actor: AuthUser, targetUsername: Username): Boolean =
    targetUsername.value == actor.username.value || actor.siteManager
