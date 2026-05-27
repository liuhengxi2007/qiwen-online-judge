package domains.auth.application



import cats.effect.IO
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.objects.request.{LoginRequest, RegisterRequest, UpdateManagedUserAccountRequest, UpdateOwnAccountRequest, UpdateUserPermissionsRequest}
import domains.auth.objects.{AuthUser, EmailAddress, PlaintextPassword, PasswordHash, SiteManagerUser}
import domains.problem.objects.ProblemTitleDisplayMode
import domains.auth.table.auth_user.AuthUserTable
import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, Username}
import domains.usergroup.application.UserGroupCommands

import java.sql.Connection

object AuthCommands:

  private val protectedAdminUsername = "admin"

  enum UpdateUserPermissionsResult:
    case Forbidden
    case ProtectedAdmin
    case NotFound
    case Updated(user: AuthUser)

  enum UpdateAccountCommand:
    case UpdateOwnAccount(actor: AuthUser, request: UpdateOwnAccountRequest)
    case UpdateManagedAccount(actor: SiteManagerUser, request: UpdateManagedUserAccountRequest)

  enum UpdateAccountResult:
    case Forbidden
    case InvalidCurrentPassword
    case NotFound
    case Updated(user: AuthUser, passwordChanged: Boolean)

  enum DeleteAccountResult:
    case Forbidden
    case ProtectedAdmin
    case CannotDeleteSelf
    case NotFound
    case HasOwnedResources
    case Deleted

  def validateUsername(username: Username): Either[String, Username] =
    UsernameRules.validate(username) match
      case Some(message) => Left(message)
      case None => Right(Username.canonical(username.value))

  def accessPolicyUserExists(connection: Connection, username: Username): IO[Boolean] =
    AuthUserTable.findByUsername(connection, username).map(_.nonEmpty)

  def resolveUserGroupMemberTarget(connection: Connection, username: Username): IO[Option[Username]] =
    AuthUserTable.findByUsername(connection, username).map(_.map(_.username))

  def usernameConflictsWithUser(connection: Connection, rawValue: String): IO[Boolean] =
    AuthUserTable.findByUsername(connection, Username.canonical(rawValue)).map(_.nonEmpty)

  def hashPassword(password: PlaintextPassword): IO[PasswordHash] =
    PasswordHasher.hashPassword(password)

  def verifyPassword(
    password: PlaintextPassword,
    passwordHash: PasswordHash
  ): IO[Boolean] =
    PasswordHasher.verifyPassword(password, passwordHash)

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

  def updateAccount(
    connection: Connection,
    targetUsername: Username,
    command: UpdateAccountCommand
  ): IO[UpdateAccountResult] =
    if !commandCanAccessTarget(command, targetUsername) then
      IO.pure(UpdateAccountResult.Forbidden)
    else
      AuthUserTable.findByUsername(connection, targetUsername).flatMap {
        case None =>
          IO.pure(UpdateAccountResult.NotFound)
        case Some(targetUser) =>
          command match
            case UpdateAccountCommand.UpdateOwnAccount(actor, request) =>
              updateOwnAccount(connection, actor, targetUser, request)
            case UpdateAccountCommand.UpdateManagedAccount(_, request) =>
              updateAccountRecord(
                connection,
                targetUser,
                email = request.email,
                newPassword = request.newPassword
              )
      }

  def deleteAccount(
    connection: Connection,
    actor: AuthUser,
    targetUsername: Username
  ): IO[DeleteAccountResult] =
    SiteManagerUser.from(actor) match
      case None =>
        IO.pure(DeleteAccountResult.Forbidden)
      case Some(_) if targetUsername.value == protectedAdminUsername =>
        IO.pure(DeleteAccountResult.ProtectedAdmin)
      case Some(_) if targetUsername.value == actor.username.value =>
        IO.pure(DeleteAccountResult.CannotDeleteSelf)
      case Some(_) =>
        AuthUserTable.delete(connection, targetUsername).map {
          case AuthUserTable.DeleteAccountTableResult.NotFound => DeleteAccountResult.NotFound
          case AuthUserTable.DeleteAccountTableResult.HasOwnedResources => DeleteAccountResult.HasOwnedResources
          case AuthUserTable.DeleteAccountTableResult.Deleted => DeleteAccountResult.Deleted
        }

  def login(connection: Connection, sessionStore: SessionStore, request: LoginRequest): IO[LoginResult] =
    AuthUserTable.findByUsername(connection, request.username).flatMap {
      case Some(foundUser) =>
        verifyPassword(request.password, foundUser.passwordHash).flatMap {
          case true =>
            sessionStore
              .createSessionInConnection(connection, foundUser.username)
              .map(sessionToken => LoginResult.LoggedIn(foundUser, sessionToken))
          case false =>
            IO.pure(LoginResult.InvalidCredentials)
        }
      case None =>
        IO.pure(LoginResult.InvalidCredentials)
    }

  def register(connection: Connection, sessionStore: SessionStore, request: RegisterRequest): IO[RegisterResult] =
    UsernameRules.validate(request.username) match
      case Some(validationError) =>
        IO.pure(RegisterResult.ValidationFailed(validationError))
      case None =>
        for
          existingUser <- AuthUserTable.findByUsername(connection, request.username)
          conflictingUserGroupSlugExists <- UserGroupCommands.userGroupSlugConflictsWith(connection, request.username.value)
          result <- existingUser match
            case Some(_) =>
              IO.pure(RegisterResult.UsernameConflict)
            case None if conflictingUserGroupSlugExists =>
              IO.pure(RegisterResult.UsernameConflictsWithUserGroup)
            case None =>
              validateRegisterRequest(request) match
                case Some(validationError) =>
                  IO.pure(RegisterResult.ValidationFailed(validationError))
                case None =>
                  hashPassword(request.password)
                    .flatMap(passwordHash =>
                      AuthUserTable
                        .insert(
                          connection,
                          username = request.username,
                          displayName = request.displayName,
                          email = request.email,
                          displayMode = UserDisplayMode.DisplayName,
                          locale = UserLocale.En,
                          problemTitleDisplayMode = ProblemTitleDisplayMode.Title,
                          autoMarkMessageRead = false,
                          passwordHash = passwordHash
                        )
                    )
                    .flatMap(createdUser =>
                      sessionStore
                        .createSessionInConnection(connection, createdUser.username)
                        .map(sessionToken => RegisterResult.Registered(createdUser, sessionToken))
                    )
        yield result

  private def validateRegisterRequest(request: RegisterRequest): Option[String] =
    validateDisplayName(request.displayName).orElse(validateEmail(request.email))

  private def validateDisplayName(displayName: DisplayName): Option[String] =
    val normalized = displayName.value.trim

    if normalized.isEmpty then Some("Display name is required.")
    else if normalized.length > 120 then Some("Display name must be at most 120 characters.")
    else None

  private def validateEmail(email: EmailAddress): Option[String] =
    val normalized = email.value.trim
    val emailPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".r

    if normalized.isEmpty then Some("Email is required.")
    else if normalized.length > 255 then Some("Email must be at most 255 characters.")
    else if emailPattern.matches(normalized) then None
    else Some("Please enter a valid email address.")

  private def updateOwnAccount(
    connection: Connection,
    actor: AuthUser,
    targetUser: AuthUser,
    request: UpdateOwnAccountRequest
  ): IO[UpdateAccountResult] =
    verifyPassword(request.currentPassword, actor.passwordHash).flatMap {
      case false =>
        IO.pure(UpdateAccountResult.InvalidCurrentPassword)
      case true =>
        updateAccountRecord(
          connection,
          targetUser,
          email = request.email,
          newPassword = request.newPassword
        )
    }

  private def updateAccountRecord(
    connection: Connection,
    targetUser: AuthUser,
    email: EmailAddress,
    newPassword: Option[PlaintextPassword]
  ): IO[UpdateAccountResult] =
    val passwordChanged = newPassword.nonEmpty
    for
      nextPasswordHash <- newPassword match
        case Some(password) => hashPassword(password)
        case None => IO.pure(targetUser.passwordHash)
      updatedUser <- AuthUserTable.updateAccount(
        connection,
        targetUser.username,
        email = email,
        passwordHash = nextPasswordHash
      )
    yield updatedUser match
      case Some(user) => UpdateAccountResult.Updated(user, passwordChanged)
      case None => UpdateAccountResult.NotFound

  private def commandCanAccessTarget(command: UpdateAccountCommand, targetUsername: Username): Boolean =
    command match
      case UpdateAccountCommand.UpdateOwnAccount(actor, _) =>
        targetUsername.value == actor.username.value
      case UpdateAccountCommand.UpdateManagedAccount(actor, _) =>
        actor.authUser.siteManager && targetUsername.value != actor.authUser.username.value
