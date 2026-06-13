package domains.auth.api

import cats.effect.IO
import domains.auth.objects.internal.{AuthAccount, AuthenticatedUser}
import domains.auth.objects.request.{UpdateManagedUserAccountRequest, UpdateOwnAccountRequest}
import domains.auth.objects.response.SessionResponse
import domains.auth.table.auth_account.AuthAccountTable
import domains.auth.utils.{AuthSessionCookies, PasswordHasher, SessionStore}
import domains.user.api.FindUserProfileSettings
import domains.user.objects.Username
import io.circe.Json
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 账号设置更新 API，支持用户自改账号和站点管理员代改账号。 */
final case class UpdateAccount(sessionStore: SessionStore) extends AuthenticatedResponseApi[(Username, Json)]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/accounts/:targetUsername/settings/account")

  /** 从路径读取目标用户名，并保留原始 JSON 以便按操作者身份选择请求类型。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, Json)] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      body <- request.as[Json]
    yield (Username.canonical(rawUsername), body)

  /** 根据目标是否为当前用户选择自助或管理员更新路径，非管理员跨用户访问返回 403。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (Username, Json)
  ): IO[Response[IO]] =
    val (targetUsername, body) = input
    if targetUsername.value == actor.username.value then
      updateOwnAccount(connection, actor, targetUsername, body)
    else if actor.siteManager then
      updateManagedAccount(connection, targetUsername, body)
    else
      HttpApiError.raise(HttpApiError.forbidden(ApiMessages.siteManagerRequired))

  /** 处理用户自改账号资料；要求当前密码正确，密码变更后清理本人会话并返回清 cookie 响应。 */
  private def updateOwnAccount(
    connection: Connection,
    actor: AuthenticatedUser,
    targetUsername: Username,
    body: Json
  ): IO[Response[IO]] =
    for
      request <- decodeBody[UpdateOwnAccountRequest](body)
      targetAccount <- findTargetAccount(connection, targetUsername)
      passwordValid <- PasswordHasher.verifyPassword(request.currentPassword, targetAccount.passwordHash)
      _ <- HttpApiError.ensure(passwordValid, HttpApiError.unauthorized(ApiMessages.invalidCurrentPassword))
      response <- updateAccountRecord(
        connection,
        targetAccount,
        request.email,
        request.newPassword,
        clearSessionOnPasswordChange = true
      )
    yield response

  /** 处理站点管理员代改账号资料；不要求目标用户当前密码，也不清理管理员自己的 cookie。 */
  private def updateManagedAccount(
    connection: Connection,
    targetUsername: Username,
    body: Json
  ): IO[Response[IO]] =
    for
      request <- decodeBody[UpdateManagedUserAccountRequest](body)
      targetAccount <- findTargetAccount(connection, targetUsername)
      response <- updateAccountRecord(
        connection,
        targetAccount,
        request.email,
        request.newPassword,
        clearSessionOnPasswordChange = false
      )
    yield response

  private def decodeBody[A: io.circe.Decoder](body: Json): IO[A] =
    body.as[A] match
      case Right(value) => IO.pure(value)
      case Left(error) => HttpApiError.raise(HttpApiError.badRequest(error.getMessage))

  private def findTargetAccount(connection: Connection, targetUsername: Username): IO[AuthAccount] =
    AuthAccountTable.findAccountByUsername(connection, targetUsername).flatMap {
      case Some(account) => IO.pure(account)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    }

  /** 写入账号邮箱和可选新密码，并在密码变更时删除目标账号所有服务端会话。 */
  private def updateAccountRecord(
    connection: Connection,
    targetAccount: AuthAccount,
    email: domains.auth.objects.EmailAddress,
    newPassword: Option[domains.auth.objects.PlaintextPassword],
    clearSessionOnPasswordChange: Boolean
  ): IO[Response[IO]] =
    val passwordChanged = newPassword.nonEmpty
    for
      nextPasswordHash <- newPassword match
        case Some(password) => PasswordHasher.hashPassword(password)
        case None => IO.pure(targetAccount.passwordHash)
      updated <- AuthAccountTable.updateAccount(
        connection,
        targetAccount.username,
        email = domains.auth.objects.EmailAddress(email.value.trim),
        passwordHash = nextPasswordHash
      )
      account <- updated match
        case Some(account) => IO.pure(account)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      profile <- FindUserProfileSettings.plan(connection, account.username).flatMap {
        case Some(profile) => IO.pure(profile)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
      _ <- if passwordChanged then sessionStore.deleteSessionsForUsername(targetAccount.username) else IO.unit
    yield
      val response =
        Response[IO](status = Status.Ok)
          .withEntity(SessionResponse.fromParts(profile, account.email, account.siteManager, account.problemManager, account.contestManager).asJson)
      if passwordChanged && clearSessionOnPasswordChange then response.addCookie(AuthSessionCookies.clearedSessionCookie)
      else response
