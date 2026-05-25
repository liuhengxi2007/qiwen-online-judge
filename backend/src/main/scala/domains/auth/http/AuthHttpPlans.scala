package domains.auth.http

import domains.auth.http.mapper.AuthHttpResponseMappers



import cats.effect.IO
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.application.AuthCommands
import domains.auth.model.request.{LoginRequest, RegisterRequest}
import domains.auth.model.response.SessionResponse
import domains.auth.model.*

import java.sql.Connection

object AuthHttpPlans:

  final case class LogoutOutput(clearedSessionCookie: org.http4s.ResponseCookie)

  case object Session extends AuthenticatedPlainAuthHttpPlan[Unit, SessionResponse]:

    override def execute(
      context: AuthHttpContext,
      actor: AuthUser,
      input: Unit
    ): IO[SessionResponse] =
      IO.pure(AuthHttpResponseMappers.toSessionResponse(actor))

  case object Logout extends PublicPlainAuthHttpPlan[Option[SessionToken], LogoutOutput]:

    override def execute(
      context: AuthHttpContext,
      input: Option[SessionToken]
    ): IO[LogoutOutput] =
      input match
        case Some(token) =>
          context.sessionStore.deleteSession(token).as(LogoutOutput(AuthHttpResponseMappers.clearedSessionCookie))
        case None =>
          IO.pure(LogoutOutput(AuthHttpResponseMappers.clearedSessionCookie))

  case object Login extends PublicTransactionAuthHttpPlan[LoginRequest, LoginResult]:

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      input: LoginRequest
    ): IO[LoginResult] =
      AuthCommands.login(connection, context.sessionStore, input)

  case object Register extends PublicTransactionAuthHttpPlan[RegisterRequest, RegisterResult]:

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      input: RegisterRequest
    ): IO[RegisterResult] =
      AuthCommands.register(connection, context.sessionStore, input)
