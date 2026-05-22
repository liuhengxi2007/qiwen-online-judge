package domains.auth.http

import domains.auth.http.response.AuthHttpResponses



import cats.effect.IO
import domains.auth.application.{PasswordHasher, UsernameRules}
import domains.auth.application.input.{LoginRequest, RegisterRequest}
import domains.auth.application.output.SessionResponse
import domains.auth.model.*
import domains.auth.table.AuthUserTable
import domains.judger.application.output.RegisteredJudgerListItem
import domains.judger.table.JudgerTable
import domains.usergroup.model.UserGroupSlug
import domains.usergroup.table.UserGroupTable

import java.sql.Connection

object AuthHttpPlans:

  enum LoginOutput:
    case InvalidCredentials
    case LoggedIn(user: AuthUser, sessionToken: SessionToken)

  enum RegisterOutput:
    case ValidationFailed(message: String)
    case UsernameConflict
    case UsernameConflictsWithUserGroup
    case Registered(user: AuthUser, sessionToken: SessionToken)

  final case class LogoutOutput(clearedSessionCookie: org.http4s.ResponseCookie)

  case object Session extends AuthenticatedPlainAuthHttpPlan[Unit, SessionResponse]:

    override val name: String = "Session"

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: Unit
    ): IO[SessionResponse] =
      IO.pure(AuthHttpResponses.toSessionResponse(actor))

  case object Logout extends PublicPlainAuthHttpPlan[Option[SessionToken], LogoutOutput]:

    override val name: String = "Logout"

    override def execute(
      context: AuthHttpContext,
      input: Option[SessionToken]
    ): IO[LogoutOutput] =
      input match
        case Some(token) =>
          context.sessionStore.deleteSession(token).as(LogoutOutput(AuthHttpResponses.clearedSessionCookie))
        case None =>
          IO.pure(LogoutOutput(AuthHttpResponses.clearedSessionCookie))

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
                    PasswordHasher
                      .hashPassword(input.password)
                      .flatMap(passwordHash =>
                        AuthUserTable
                          .insert(
                            connection,
                            username = input.username,
                            displayName = input.displayName,
                            email = input.email,
                            displayMode = domains.user.model.UserDisplayMode.DisplayName,
                            locale = domains.user.model.UserLocale.En,
                            problemTitleDisplayMode = domains.problem.model.ProblemTitleDisplayMode.Title,
                            autoMarkMessageRead = false,
                            passwordHash = passwordHash
                          )
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
