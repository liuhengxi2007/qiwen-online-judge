package domains.auth.api

import cats.effect.IO
import domains.auth.objects.AuthUser
import domains.auth.objects.request.{UpdateManagedUserAccountRequest, UpdateOwnAccountRequest}
import domains.auth.objects.response.SessionResponse
import domains.auth.table.auth_user.AuthUserTable
import domains.auth.utils.{AuthSessionCookies, PasswordHasher, SessionStore}
import domains.user.objects.Username
import io.circe.Json
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class UpdateAccount(sessionStore: SessionStore) extends AuthenticatedResponseApi[(Username, Json)]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/accounts/:targetUsername/settings/account")

  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, Json)] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      body <- request.as[Json]
    yield (Username.canonical(rawUsername), body)

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: (Username, Json)
  ): IO[Response[IO]] =
    val (targetUsername, body) = input
    if targetUsername.value == actor.username.value then
      updateOwnAccount(connection, actor, targetUsername, body)
    else if actor.siteManager then
      updateManagedAccount(connection, targetUsername, body)
    else
      HttpApiError.raise(HttpApiError.forbidden(ApiMessages.siteManagerRequired))

  private def updateOwnAccount(
    connection: Connection,
    actor: AuthUser,
    targetUsername: Username,
    body: Json
  ): IO[Response[IO]] =
    for
      request <- decodeBody[UpdateOwnAccountRequest](body)
      targetUser <- findTargetUser(connection, targetUsername)
      passwordValid <- PasswordHasher.verifyPassword(request.currentPassword, actor.passwordHash)
      _ <- HttpApiError.ensure(passwordValid, HttpApiError.unauthorized(ApiMessages.invalidCurrentPassword))
      response <- updateAccountRecord(
        connection,
        targetUser,
        request.email,
        request.newPassword,
        clearSessionOnPasswordChange = true
      )
    yield response

  private def updateManagedAccount(
    connection: Connection,
    targetUsername: Username,
    body: Json
  ): IO[Response[IO]] =
    for
      request <- decodeBody[UpdateManagedUserAccountRequest](body)
      targetUser <- findTargetUser(connection, targetUsername)
      response <- updateAccountRecord(
        connection,
        targetUser,
        request.email,
        request.newPassword,
        clearSessionOnPasswordChange = false
      )
    yield response

  private def decodeBody[A: io.circe.Decoder](body: Json): IO[A] =
    body.as[A] match
      case Right(value) => IO.pure(value)
      case Left(error) => HttpApiError.raise(HttpApiError.badRequest(error.getMessage))

  private def findTargetUser(connection: Connection, targetUsername: Username): IO[AuthUser] =
    AuthUserTable.findByUsername(connection, targetUsername).flatMap {
      case Some(user) => IO.pure(user)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    }

  private def updateAccountRecord(
    connection: Connection,
    targetUser: AuthUser,
    email: domains.auth.objects.EmailAddress,
    newPassword: Option[domains.auth.objects.PlaintextPassword],
    clearSessionOnPasswordChange: Boolean
  ): IO[Response[IO]] =
    val passwordChanged = newPassword.nonEmpty
    for
      nextPasswordHash <- newPassword match
        case Some(password) => PasswordHasher.hashPassword(password)
        case None => IO.pure(targetUser.passwordHash)
      updated <- AuthUserTable.updateAccount(
        connection,
        targetUser.username,
        email = domains.auth.objects.EmailAddress(email.value.trim),
        passwordHash = nextPasswordHash
      )
      user <- updated match
        case Some(user) => IO.pure(user)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      _ <- if passwordChanged then sessionStore.deleteSessionsForUsername(targetUser.username) else IO.unit
    yield
      val response = Response[IO](status = Status.Ok).withEntity(SessionResponse.fromAuthUser(user).asJson)
      if passwordChanged && clearSessionOnPasswordChange then response.addCookie(AuthSessionCookies.clearedSessionCookie)
      else response
