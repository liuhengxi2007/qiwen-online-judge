package domains.auth.http

import domains.auth.http.mapper.AuthHttpResponseMappers



import cats.effect.IO
import domains.auth.application.AuthCommandResults.{LoginResult, RegisterResult}
import domains.auth.application.AuthCommands
import domains.auth.model.request.{LoginRequest, RegisterRequest, UpdateManagedUserAccountRequest, UpdateOwnAccountRequest, UpdateUserPermissionsRequest}
import domains.auth.model.response.SessionResponse
import domains.auth.model.*
import domains.user.model.Username

import java.sql.Connection

object AuthHttpPlans:

  final case class LogoutOutput(clearedSessionCookie: org.http4s.ResponseCookie)

  final case class UpdateAccountOutput(
    result: AuthCommands.UpdateAccountResult,
    clearSessionCookie: Boolean
  )

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

  case object UpdateUserPermissions
      extends SiteManagerTransactionAuthHttpPlan[(Username, UpdateUserPermissionsRequest), AuthCommands.UpdateUserPermissionsResult]:

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateUserPermissionsRequest)
    ): IO[AuthCommands.UpdateUserPermissionsResult] =
      val _ = context
      val (targetUsername, request) = input
      AuthCommands.updateUserPermissions(connection, actor.authUser, targetUsername, request)

  case object UpdateOwnAccount
      extends AuthenticatedTransactionAuthHttpPlan[(Username, UpdateOwnAccountRequest), UpdateAccountOutput]:

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: AuthUser,
      input: (Username, UpdateOwnAccountRequest)
    ): IO[UpdateAccountOutput] =
      val (targetUsername, request) = input
      val command = AuthCommands.UpdateAccountCommand.UpdateOwnAccount(actor, request)
      for
        result <- AuthCommands.updateAccount(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield
        UpdateAccountOutput(
          result = result,
          clearSessionCookie = passwordChangedByActor(actor, targetUsername, result)
        )

  case object UpdateManagedAccount
      extends SiteManagerTransactionAuthHttpPlan[(Username, UpdateManagedUserAccountRequest), UpdateAccountOutput]:

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: (Username, UpdateManagedUserAccountRequest)
    ): IO[UpdateAccountOutput] =
      val (targetUsername, request) = input
      val command = AuthCommands.UpdateAccountCommand.UpdateManagedAccount(actor, request)
      for
        result <- AuthCommands.updateAccount(connection, targetUsername, command)
        _ <- revokePasswordChangedSessions(context, targetUsername, result)
      yield UpdateAccountOutput(result = result, clearSessionCookie = false)

  case object DeleteAccount extends SiteManagerTransactionAuthHttpPlan[Username, AuthCommands.DeleteAccountResult]:

    override def execute(
      context: AuthHttpContext,
      connection: Connection,
      actor: SiteManagerUser,
      input: Username
    ): IO[AuthCommands.DeleteAccountResult] =
      val _ = context
      AuthCommands.deleteAccount(connection, actor.authUser, input)

  private def revokePasswordChangedSessions(
    context: AuthHttpContext,
    targetUsername: Username,
    result: AuthCommands.UpdateAccountResult
  ): IO[Unit] =
    result match
      case AuthCommands.UpdateAccountResult.Updated(_, true) =>
        context.sessionStore.deleteSessionsForUsername(targetUsername)
      case _ =>
        IO.unit

  private def passwordChangedByActor(
    actor: AuthUser,
    targetUsername: Username,
    result: AuthCommands.UpdateAccountResult
  ): Boolean =
    result match
      case AuthCommands.UpdateAccountResult.Updated(_, true) =>
        actor.username.value == targetUsername.value
      case _ =>
        false
