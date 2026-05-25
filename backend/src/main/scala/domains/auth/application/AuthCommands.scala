package domains.auth.application



import cats.effect.IO
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.model.request.{LoginRequest, RegisterRequest}
import domains.auth.model.{EmailAddress, PlaintextPassword, PasswordHash}
import domains.problem.model.ProblemTitleDisplayMode
import domains.auth.table.auth_user.AuthUserTable
import domains.user.model.{DisplayName, UserDisplayMode, UserLocale, Username}
import domains.usergroup.application.UserGroupCommands

import java.sql.Connection

object AuthCommands:

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
