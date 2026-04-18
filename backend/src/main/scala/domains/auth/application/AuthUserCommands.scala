package domains.auth.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, SiteManagerUser, UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest, UserDisplayMode, UserLocale, Username}
import domains.auth.table.AuthUserTable
import domains.problem.model.ProblemTitleDisplayMode

import java.sql.Connection

object AuthUserCommands:

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
    case UpdateOwn(actor: AuthUser, request: UpdateOwnSettingsRequest)
    case UpdateManaged(actor: SiteManagerUser, request: UpdateManagedUserSettingsRequest)

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
    databaseSession.withTransactionConnection(connection =>
      AuthUserTable.findByUsername(connection, targetUsername)
    ).map {
      case Some(targetUser) => GetUserSettingsResult.Found(targetUser)
      case None => GetUserSettingsResult.NotFound
    }

  def updateUserPermissions(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    targetUsername: Username,
    permissionsRequest: UpdateUserPermissionsRequest
  ): IO[UpdateUserPermissionsResult] =
    databaseSession.withTransactionConnection(connection =>
      updateUserPermissions(connection, actor, targetUsername, permissionsRequest)
    )

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
          AuthUserTable.updatePermissions(
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
    databaseSession: DatabaseSession,
    targetUsername: Username,
    command: UpdateUserSettingsCommand
  ): IO[UpdateUserSettingsResult] =
    databaseSession.withTransactionConnection(connection =>
      updateUserSettings(connection, targetUsername, command)
    )

  def updateUserSettings(
    connection: Connection,
    targetUsername: Username,
    command: UpdateUserSettingsCommand
  ): IO[UpdateUserSettingsResult] =
    if !commandCanAccessTarget(command, targetUsername) then
      IO.pure(UpdateUserSettingsResult.Forbidden)
    else
      AuthUserTable.findByUsername(connection, targetUsername).flatMap {
        case None =>
          IO.pure(UpdateUserSettingsResult.NotFound)
        case Some(targetUser) =>
          command match
            case UpdateUserSettingsCommand.UpdateOwn(actor, request) =>
              updateOwnSettings(connection, actor, targetUser, request)
            case UpdateUserSettingsCommand.UpdateManaged(_, request) =>
              updateSettingsRecord(
                connection,
                targetUser,
                request.displayName,
                request.email,
                request.preferences.displayMode,
                request.preferences.locale,
                request.preferences.problemTitleDisplayMode,
                request.newPassword
              )
      }

  private def updateOwnSettings(
    connection: Connection,
    actor: AuthUser,
    targetUser: AuthUser,
    request: UpdateOwnSettingsRequest
  ): IO[UpdateUserSettingsResult] =
    PasswordHasher.verifyPassword(request.currentPassword, actor.passwordHash).flatMap {
      case false =>
        IO.pure(UpdateUserSettingsResult.InvalidCurrentPassword)
      case true =>
        updateSettingsRecord(
          connection,
          targetUser,
          request.displayName,
          request.email,
          request.preferences.displayMode,
          request.preferences.locale,
          request.preferences.problemTitleDisplayMode,
          request.newPassword
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
      updatedUser <- AuthUserTable.updateSettings(
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

  def deleteUser(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    targetUsername: Username
  ): IO[DeleteUserResult] =
    databaseSession.withTransactionConnection(connection =>
      deleteUser(connection, actor, targetUsername)
    )

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
        AuthUserTable.delete(connection, targetUsername).map {
          case AuthUserTable.DeleteUserTableResult.NotFound => DeleteUserResult.NotFound
          case AuthUserTable.DeleteUserTableResult.HasOwnedResources => DeleteUserResult.HasOwnedResources
          case AuthUserTable.DeleteUserTableResult.Deleted => DeleteUserResult.Deleted
        }

  private def commandCanAccessTarget(command: UpdateUserSettingsCommand, targetUsername: Username): Boolean =
    command match
      case UpdateUserSettingsCommand.UpdateOwn(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateManaged(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value
