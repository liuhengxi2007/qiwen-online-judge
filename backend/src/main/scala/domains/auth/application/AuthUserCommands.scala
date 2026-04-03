package domains.auth.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, SiteManagerUser, UpdateManagedUserSettingsRequest, UpdateOwnSettingsRequest, UpdateUserPermissionsRequest, Username}
import domains.auth.table.AuthUserTable

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
    if !canAccessTarget(actor, targetUsername) then
      IO.pure(GetUserSettingsResult.Forbidden)
    else
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
    SiteManagerUser.from(actor) match
      case None =>
        IO.pure(UpdateUserPermissionsResult.Forbidden)
      case Some(siteManagerActor) =>
        if targetUsername.value == protectedAdminUsername then
          IO.pure(UpdateUserPermissionsResult.ProtectedAdmin)
        else
          databaseSession.withTransactionConnection(connection =>
            AuthUserTable.updatePermissions(
              connection,
              siteManagerActor,
              targetUsername,
              siteManager = permissionsRequest.siteManager,
              problemManager = permissionsRequest.problemManager
            )
          ).map {
            case Some(updatedUser) => UpdateUserPermissionsResult.Updated(updatedUser)
            case None => UpdateUserPermissionsResult.NotFound
          }

  def updateUserSettings(
    databaseSession: DatabaseSession,
    targetUsername: Username,
    command: UpdateUserSettingsCommand
  ): IO[UpdateUserSettingsResult] =
    if !commandCanAccessTarget(command, targetUsername) then
      IO.pure(UpdateUserSettingsResult.Forbidden)
    else
      databaseSession.withTransactionConnection(connection =>
        AuthUserTable.findByUsername(connection, targetUsername)
      ).flatMap {
        case None =>
          IO.pure(UpdateUserSettingsResult.NotFound)
        case Some(targetUser) =>
          command match
            case UpdateUserSettingsCommand.UpdateOwn(actor, request) =>
              updateOwnSettings(databaseSession, actor, targetUser, request)
            case UpdateUserSettingsCommand.UpdateManaged(_, request) =>
              updateSettingsRecord(
                databaseSession,
                targetUser,
                request.displayName,
                request.email,
                request.newPassword
              )
      }

  private def updateOwnSettings(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    targetUser: AuthUser,
    request: UpdateOwnSettingsRequest
  ): IO[UpdateUserSettingsResult] =
    PasswordHasher.verifyPassword(request.currentPassword, actor.passwordHash).flatMap {
      case false =>
        IO.pure(UpdateUserSettingsResult.InvalidCurrentPassword)
      case true =>
        updateSettingsRecord(databaseSession, targetUser, request.displayName, request.email, request.newPassword)
    }

  private def updateSettingsRecord(
    databaseSession: DatabaseSession,
    targetUser: AuthUser,
    displayName: domains.auth.model.DisplayName,
    email: domains.auth.model.EmailAddress,
    newPassword: Option[domains.auth.model.PlaintextPassword]
  ): IO[UpdateUserSettingsResult] =
    val passwordChanged = newPassword.nonEmpty
    for
      nextPasswordHash <- newPassword match
        case Some(password) => PasswordHasher.hashPassword(password)
        case None => IO.pure(targetUser.passwordHash)
      updatedUser <- databaseSession.withTransactionConnection(connection =>
        AuthUserTable.updateSettings(
          connection,
          targetUser.username,
          displayName = displayName,
          email = email,
          passwordHash = nextPasswordHash
        )
      )
    yield updatedUser match
      case Some(user) => UpdateUserSettingsResult.Updated(user, passwordChanged)
      case None => UpdateUserSettingsResult.NotFound

  def deleteUser(
    databaseSession: DatabaseSession,
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
        databaseSession.withTransactionConnection(connection =>
          AuthUserTable.delete(connection, targetUsername)
        ).map {
          case AuthUserTable.DeleteUserTableResult.NotFound => DeleteUserResult.NotFound
          case AuthUserTable.DeleteUserTableResult.HasOwnedResources => DeleteUserResult.HasOwnedResources
          case AuthUserTable.DeleteUserTableResult.Deleted => DeleteUserResult.Deleted
        }

  private def canAccessTarget(actor: AuthUser, targetUsername: Username): Boolean =
    targetUsername.value == actor.username.value || actor.siteManager

  private def commandCanAccessTarget(command: UpdateUserSettingsCommand, targetUsername: Username): Boolean =
    command match
      case UpdateUserSettingsCommand.UpdateOwn(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateUserSettingsCommand.UpdateManaged(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value
