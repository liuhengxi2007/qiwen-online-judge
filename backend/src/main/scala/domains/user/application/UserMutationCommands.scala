package domains.user.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.PasswordHasher
import domains.auth.model.{AuthUser, DisplayName, EmailAddress, PlaintextPassword, SiteManagerUser, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.user.model.{UpdateManagedUserAccountRequest, UpdateManagedUserPreferencesRequest, UpdateManagedUserProfileRequest, UpdateOwnAccountRequest, UpdateOwnPreferencesRequest, UpdateOwnProfileRequest, UpdateUserPermissionsRequest, UserDisplayMode, UserLocale, UserPreferences}
import domains.user.table.UserTable

import java.sql.Connection

object UserMutationCommands:

  private val protectedAdminUsername = "admin"

  enum GetUserSettingsResult:
    case Forbidden
    case NotFound
    case Found(user: AuthUser)

  enum UpdateUserPermissionsResult:
    case Forbidden
    case ProtectedAdmin
    case NotFound
    case Updated(user: AuthUser)

  enum UpdateUserSettingsCommand:
    case UpdateOwnProfile(actor: AuthUser, request: UpdateOwnProfileRequest)
    case UpdateOwnPreferences(actor: AuthUser, request: UpdateOwnPreferencesRequest)
    case UpdateOwnAccount(actor: AuthUser, request: UpdateOwnAccountRequest)
    case UpdateManagedProfile(actor: SiteManagerUser, request: UpdateManagedUserProfileRequest)
    case UpdateManagedPreferences(actor: SiteManagerUser, request: UpdateManagedUserPreferencesRequest)
    case UpdateManagedAccount(actor: SiteManagerUser, request: UpdateManagedUserAccountRequest)

  enum UpdateUserSettingsResult:
    case Forbidden
    case InvalidCurrentPassword
    case NotFound
    case Updated(user: AuthUser, passwordChanged: Boolean)

  enum DeleteUserResult:
    case Forbidden
    case ProtectedAdmin
    case CannotDeleteSelf
    case NotFound
    case HasOwnedResources
    case Deleted

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

  def updateUserPermissions(
    connection: Connection,
    actor: AuthUser,
    targetUsername: Username,
    permissionsRequest: UpdateUserPermissionsRequest
  ): IO[UpdateUserPermissionsResult] =
    SiteManagerUser.from(actor) match
      case None =>
        IO.pure(UpdateUserPermissionsResult.Forbidden)
      case Some(siteManagerActor) =>
        if targetUsername.value == protectedAdminUsername then
          IO.pure(UpdateUserPermissionsResult.ProtectedAdmin)
        else
          UserTable.updatePermissions(
            connection,
            siteManagerActor,
            targetUsername,
            siteManager = permissionsRequest.siteManager,
            problemManager = permissionsRequest.problemManager
          ).map {
            case Some(updatedUser) => UpdateUserPermissionsResult.Updated(updatedUser)
            case None => UpdateUserPermissionsResult.NotFound
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
                email = targetUser.email,
                displayMode = targetUser.displayMode,
                locale = targetUser.locale,
                problemTitleDisplayMode = targetUser.problemTitleDisplayMode,
                newPassword = None
              )
            case UpdateUserSettingsCommand.UpdateOwnPreferences(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = targetUser.displayName,
                email = targetUser.email,
                displayMode = request.preferences.displayMode,
                locale = request.preferences.locale,
                problemTitleDisplayMode = request.preferences.problemTitleDisplayMode,
                newPassword = None
              )
            case UpdateUserSettingsCommand.UpdateOwnAccount(actor, request) =>
              updateOwnAccount(connection, actor, targetUser, request)
            case UpdateUserSettingsCommand.UpdateManagedProfile(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = request.displayName,
                email = targetUser.email,
                displayMode = targetUser.displayMode,
                locale = targetUser.locale,
                problemTitleDisplayMode = targetUser.problemTitleDisplayMode,
                newPassword = None
              )
            case UpdateUserSettingsCommand.UpdateManagedPreferences(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = targetUser.displayName,
                email = targetUser.email,
                displayMode = request.preferences.displayMode,
                locale = request.preferences.locale,
                problemTitleDisplayMode = request.preferences.problemTitleDisplayMode,
                newPassword = None
              )
            case UpdateUserSettingsCommand.UpdateManagedAccount(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                displayName = targetUser.displayName,
                email = request.email,
                displayMode = targetUser.displayMode,
                locale = targetUser.locale,
                problemTitleDisplayMode = targetUser.problemTitleDisplayMode,
                newPassword = request.newPassword
              )
      }

  def deleteUser(
    connection: Connection,
    actor: AuthUser,
    targetUsername: Username
  ): IO[DeleteUserResult] =
    SiteManagerUser.from(actor) match
      case None =>
        IO.pure(DeleteUserResult.Forbidden)
      case Some(_) if targetUsername.value == protectedAdminUsername =>
        IO.pure(DeleteUserResult.ProtectedAdmin)
      case Some(_) if targetUsername.value == actor.username.value =>
        IO.pure(DeleteUserResult.CannotDeleteSelf)
      case Some(_) =>
        UserTable.delete(connection, targetUsername).map {
          case UserTable.DeleteUserTableResult.NotFound => DeleteUserResult.NotFound
          case UserTable.DeleteUserTableResult.HasOwnedResources => DeleteUserResult.HasOwnedResources
          case UserTable.DeleteUserTableResult.Deleted => DeleteUserResult.Deleted
        }

  private def updateOwnAccount(
    connection: Connection,
    actor: AuthUser,
    targetUser: AuthUser,
    request: UpdateOwnAccountRequest
  ): IO[UpdateUserSettingsResult] =
    PasswordHasher.verifyPassword(request.currentPassword, actor.passwordHash).flatMap {
      case false =>
        IO.pure(UpdateUserSettingsResult.InvalidCurrentPassword)
      case true =>
        updateSettingsRecord(
          connection,
          targetUser,
          displayName = targetUser.displayName,
          email = request.email,
          displayMode = targetUser.displayMode,
          locale = targetUser.locale,
          problemTitleDisplayMode = targetUser.problemTitleDisplayMode,
          newPassword = request.newPassword
        )
    }

  private def updateSettingsRecord(
    connection: Connection,
    targetUser: AuthUser,
    displayName: domains.auth.model.DisplayName,
    email: domains.auth.model.EmailAddress,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    newPassword: Option[domains.auth.model.PlaintextPassword]
  ): IO[UpdateUserSettingsResult] =
    val passwordChanged = newPassword.nonEmpty
    for
      nextPasswordHash <- newPassword match
        case Some(password) => PasswordHasher.hashPassword(password)
        case None => IO.pure(targetUser.passwordHash)
      updatedUser <- UserTable.updateSettings(
        connection,
        targetUser.username,
        displayName = displayName,
        email = email,
        displayMode = displayMode,
        locale = locale,
        problemTitleDisplayMode = problemTitleDisplayMode,
        passwordHash = nextPasswordHash
      )
    yield updatedUser match
      case Some(user) => UpdateUserSettingsResult.Updated(user, passwordChanged)
      case None => UpdateUserSettingsResult.NotFound

  private def commandCanAccessTarget(command: UpdateUserSettingsCommand, targetUsername: Username): Boolean =
    command match
      case UpdateUserSettingsCommand.UpdateOwnProfile(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateOwnPreferences(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateOwnAccount(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateManagedProfile(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value
      case UpdateUserSettingsCommand.UpdateManagedPreferences(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value
      case UpdateUserSettingsCommand.UpdateManagedAccount(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value

  private def canReadUserSettings(actor: AuthUser, targetUsername: Username): Boolean =
    targetUsername.value == actor.username.value || actor.siteManager
