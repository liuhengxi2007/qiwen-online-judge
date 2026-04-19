package domains.auth.http

import cats.effect.IO
import io.circe.Json
import domains.auth.application.{AuthUserCommands, PasswordHasher, UsernameRules}
import domains.auth.model.*
import domains.auth.table.AuthUserTable
import domains.judger.model.RegisteredJudgerListItem
import domains.judger.table.JudgerTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.model.UserGroupSlug
import domains.usergroup.table.UserGroupTable

import java.sql.Connection

object AuthHttpPlans:

  enum LoginOutput:
    case InvalidCredentials
    case LoggedIn(user: AuthUser, sessionToken: String)

  enum RegisterOutput:
    case ValidationFailed(message: String)
    case UsernameConflict
    case UsernameConflictsWithUserGroup
    case Registered(user: AuthUser, sessionToken: String)

  final case class LogoutOutput(clearedSessionCookie: org.http4s.ResponseCookie)

  enum UpdateUserSettingsOutput:
    case ValidationFailed(message: String)
    case Completed(
      result: AuthUserCommands.UpdateUserSettingsResult,
      clearCurrentSessionCookie: Boolean
    )

  case object Session extends AuthenticatedPlainAuthHttpPlan[Unit, SessionResponse]:

    override val name: String = "Session"

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: Unit
    ): IO[SessionResponse] =
      IO.pure(AuthHttpResponses.toSessionResponse(actor))

  case object Logout extends PublicPlainAuthHttpPlan[Option[String], LogoutOutput]:

    override val name: String = "Logout"

    override def execute(
      context: AuthHttpContext,
      input: Option[String]
    ): IO[LogoutOutput] =
      input match
        case Some(token) =>
          context.sessionStore.deleteSession(token).as(LogoutOutput(AuthHttpResponses.clearedSessionCookie))
        case None =>
          IO.pure(LogoutOutput(AuthHttpResponses.clearedSessionCookie))

  case object ListUsers extends SiteManagerPlainAuthHttpPlan[Unit, List[AuthUserListItem]]:

    override val name: String = "ListUsers"

    override def execute(
      context: AuthHttpContext,
      actor: SiteManagerUser,
      input: Unit
    ): IO[List[AuthUserListItem]] =
      context.databaseSession.withTransactionConnection(connection =>
        AuthUserTable.listUsers(connection, actor)
      )

  case object ListJudgers extends SiteManagerPlainAuthHttpPlan[Unit, List[RegisteredJudgerListItem]]:

    override val name: String = "ListJudgers"

    override def execute(
      context: AuthHttpContext,
      actor: SiteManagerUser,
      input: Unit
    ): IO[List[RegisteredJudgerListItem]] =
      val _ = actor
      context.databaseSession.withTransactionConnection(connection =>
        JudgerTable.listJudgers(connection, context.judgeConfig.heartbeatTimeoutMs)
      )

  case object GetUserSettings extends AuthenticatedPlainAuthHttpPlan[Username, AuthUserCommands.GetUserSettingsResult]:

    override val name: String = "GetUserSettings"

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: Username
    ): IO[AuthUserCommands.GetUserSettingsResult] =
      AuthUserCommands.getUserSettings(context.databaseSession, actor, input)

  case object GetUserProfile extends AuthenticatedPlainAuthHttpPlan[Username, AuthUserCommands.GetUserProfileResult]:

    override val name: String = "GetUserProfile"

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: Username
    ): IO[AuthUserCommands.GetUserProfileResult] =
      AuthUserCommands.getUserProfile(context.databaseSession, actor, input)

  case object ListContributionRanklist extends AuthenticatedPlainAuthHttpPlan[PageRequest, PageResponse[UserRanklistItem]]:

    override val name: String = "ListContributionRanklist"

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[UserRanklistItem]] =
      AuthUserCommands.listContributionRanklist(context.databaseSession, actor, input)

  case object ListAcceptedRanklist extends AuthenticatedPlainAuthHttpPlan[PageRequest, PageResponse[UserAcceptedRanklistItem]]:

    override val name: String = "ListAcceptedRanklist"

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: PageRequest
    ): IO[PageResponse[UserAcceptedRanklistItem]] =
      AuthUserCommands.listAcceptedRanklist(context.databaseSession, actor, input)

  case object UpdateUserPermissions
      extends SiteManagerTransactionAuthHttpPlan[(Username, UpdateUserPermissionsRequest), AuthUserCommands.UpdateUserPermissionsResult]:

    override val name: String = "UpdateUserPermissions"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateUserPermissionsRequest)
    ): IO[AuthUserCommands.UpdateUserPermissionsResult] =
      val (targetUsername, request) = input
      AuthUserCommands.updateUserPermissions(connection, actor.authUser, targetUsername, request)

  case object UpdateUserSettings
      extends AuthenticatedTransactionAuthHttpPlan[(Username, Json), UpdateUserSettingsOutput]:

    override val name: String = "UpdateUserSettings"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, Json)
    ): IO[UpdateUserSettingsOutput] =
      val (targetUsername, json) = input
      val commandResult: Either[String, AuthUserCommands.UpdateUserSettingsCommand] =
        if targetUsername.value == actor.username.value then
          json
            .as[UpdateOwnSettingsRequest]
            .left
            .map(_.getMessage)
            .map(request => AuthUserCommands.UpdateUserSettingsCommand.UpdateOwn(actor, request))
        else
          json
            .as[UpdateManagedUserSettingsRequest]
            .left
            .map(_.getMessage)
            .flatMap(request =>
              SiteManagerUser
                .from(actor)
                .toRight("Site manager permission required.")
                .map(siteManagerActor => AuthUserCommands.UpdateUserSettingsCommand.UpdateManaged(siteManagerActor, request))
            )

      commandResult match
        case Left(message) =>
          IO.pure(UpdateUserSettingsOutput.ValidationFailed(message))
        case Right(command) =>
          for
            result <- AuthUserCommands.updateUserSettings(connection, targetUsername, command)
            _ <- revokePasswordChangedSessions(context, targetUsername, result)
          yield UpdateUserSettingsOutput.Completed(
            result = result,
            clearCurrentSessionCookie = passwordChangedByActor(actor, targetUsername, result)
          )

  case object DeleteUser extends SiteManagerTransactionAuthHttpPlan[Username, AuthUserCommands.DeleteUserResult]:

    override val name: String = "DeleteUser"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: Username
    ): IO[AuthUserCommands.DeleteUserResult] =
      AuthUserCommands.deleteUser(connection, actor.authUser, input)

  case object Login extends PublicTransactionAuthHttpPlan[LoginRequest, LoginOutput]:

    override val name: String = "Login"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      input: LoginRequest
    ): IO[LoginOutput] =
      AuthUserTable.findByUsername(connection, input.username).flatMap {
        case Some(foundUser) =>
          PasswordHasher.verifyPassword(input.password, foundUser.passwordHash).flatMap {
            case true =>
              context.sessionStore
                .createSessionInConnection(connection, foundUser.username)
                .map(sessionToken => LoginOutput.LoggedIn(foundUser, sessionToken))
            case false =>
              IO.pure(LoginOutput.InvalidCredentials)
          }
        case None =>
          IO.pure(LoginOutput.InvalidCredentials)
      }

  case object Register extends PublicTransactionAuthHttpPlan[RegisterRequest, RegisterOutput]:

    override val name: String = "Register"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      input: RegisterRequest
    ): IO[RegisterOutput] =
      UsernameRules.validate(input.username) match
        case Some(validationError) =>
          IO.pure(RegisterOutput.ValidationFailed(validationError))
        case None =>
          for
            existingUser <- AuthUserTable.findByUsername(connection, input.username)
            existingUserGroup <- findConflictingUserGroup(connection, input.username)
            result <- existingUser match
              case Some(_) =>
                IO.pure(RegisterOutput.UsernameConflict)
              case None if existingUserGroup.nonEmpty =>
                IO.pure(RegisterOutput.UsernameConflictsWithUserGroup)
              case None =>
                validateRegisterRequest(input) match
                  case Some(validationError) =>
                    IO.pure(RegisterOutput.ValidationFailed(validationError))
                  case None =>
                    AuthUserTable
                      .insert(
                        connection,
                        username = input.username,
                        displayName = input.displayName,
                        email = input.email,
                        displayMode = domains.auth.model.UserDisplayMode.DisplayName,
                        locale = domains.auth.model.UserLocale.En,
                        problemTitleDisplayMode = domains.problem.model.ProblemTitleDisplayMode.Title,
                        password = input.password
                      )
                      .flatMap(createdUser =>
                        context.sessionStore
                          .createSessionInConnection(connection, createdUser.username)
                          .map(sessionToken => RegisterOutput.Registered(createdUser, sessionToken))
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

  private def findConflictingUserGroup(
    connection: Connection,
    username: Username
  ): IO[Option[domains.usergroup.model.UserGroup]] =
    UserGroupSlug.parse(username.value) match
      case Left(_) => IO.pure(None)
      case Right(slug) => UserGroupTable.findBySlug(connection, slug)

  private def revokePasswordChangedSessions(
    context: AuthHttpContext,
    targetUsername: Username,
    result: AuthUserCommands.UpdateUserSettingsResult
  ): IO[Unit] =
    result match
      case AuthUserCommands.UpdateUserSettingsResult.Updated(_, true) =>
        context.sessionStore.deleteSessionsForUsername(targetUsername)
      case _ =>
        IO.unit

  private def passwordChangedByActor(
    actor: AuthUser,
    targetUsername: Username,
    result: AuthUserCommands.UpdateUserSettingsResult
  ): Boolean =
    result match
      case AuthUserCommands.UpdateUserSettingsResult.Updated(_, true) =>
        actor.username.value == targetUsername.value
      case _ =>
        false
