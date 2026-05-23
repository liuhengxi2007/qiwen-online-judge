package domains.auth.http

import domains.auth.http.response.AuthHttpResponses



import cats.effect.IO
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.application.AuthCommands
import domains.auth.application.input.{LoginRequest, RegisterRequest}
import domains.auth.application.output.SessionResponse
import domains.auth.model.*

import java.sql.Connection

object AuthHttpPlans:

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

  case object Login extends PublicTransactionAuthHttpPlan[LoginRequest, LoginResult]:

    override val name: String = "Login"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      input: LoginRequest
    ): IO[LoginResult] =
      AuthCommands.login(connection, context.sessionStore, input)

  case object Register extends PublicTransactionAuthHttpPlan[RegisterRequest, RegisterResult]:

    override val name: String = "Register"

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      input: RegisterRequest
    ): IO[RegisterResult] =
      AuthCommands.register(connection, context.sessionStore, input)
