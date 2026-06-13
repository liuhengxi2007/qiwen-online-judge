package domains.auth.api

import cats.effect.IO
import domains.auth.objects.EmailAddress
import domains.auth.objects.request.RegisterRequest
import domains.auth.objects.response.RegisterResponse
import domains.auth.table.auth_account.AuthAccountTable
import domains.auth.utils.{AuthSessionCookies, PasswordHasher, SessionStore}
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.api.CreateUserProfileSettings
import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, Username}
import domains.usergroup.api.ResolveUserGroupSlug
import domains.usergroup.objects.UserGroupSlug
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import io.circe.syntax.*

import java.sql.Connection

/** 注册 API，创建账号与用户资料，并直接建立登录会话。 */
final case class Register(sessionStore: SessionStore) extends PublicResponseApi[RegisterRequest]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/register")

  /** 从 JSON 请求体解码注册信息；路径参数被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[RegisterRequest] =
    val _ = pathParams
    request.as[RegisterRequest]

  /** 校验用户名/展示名/邮箱唯一性和格式，写入账号与资料后返回注册响应和 cookie。 */
  override def plan(connection: Connection, request: RegisterRequest): IO[Response[IO]] =
    for
      username <- HttpApiError.fromEitherBadRequest(Username.parse(request.username.value))
      validRequest = request.copy(username = username)
      existingUser <- AuthAccountTable.findAccountByUsername(connection, validRequest.username)
      _ <- HttpApiError.ensure(existingUser.isEmpty, HttpApiError.conflict(ApiMessages.usernameExists))
      conflictingUserGroupSlug <- userGroupSlugConflictsWith(connection, validRequest.username.value)
      _ <- HttpApiError.ensure(!conflictingUserGroupSlug, HttpApiError.conflict(ApiMessages.usernameConflictsWithGroup))
      displayName <- validateDisplayName(validRequest.displayName)
      email <- validateEmail(validRequest.email)
      passwordHash <- PasswordHasher.hashPassword(validRequest.password)
      createdAccount <- AuthAccountTable.insertAccount(
        connection,
        username = validRequest.username,
        email = email,
        passwordHash = passwordHash
      )
      profile <- CreateUserProfileSettings.plan(
        connection,
        CreateUserProfileSettings.Input(
          username = createdAccount.username,
          displayName = displayName,
          displayMode = UserDisplayMode.DisplayName,
          locale = UserLocale.En,
          problemTitleDisplayMode = ProblemTitleDisplayMode.Title,
          autoMarkMessageRead = false
        )
      )
      sessionToken <- sessionStore.createSessionInConnection(connection, createdAccount.username)
    yield
      Response[IO](status = Status.Created)
        .withEntity(
          RegisterResponse
            .fromParts(
              profile,
              createdAccount.email,
              createdAccount.siteManager,
              createdAccount.problemManager,
              createdAccount.contestManager,
              "Registration successful"
            )
            .asJson
        )
        .addCookie(AuthSessionCookies.sessionCookie(sessionToken))

  /** 检查待注册用户名是否也能解析为用户组 slug 且已被用户组占用，避免公开标识冲突。 */
  private def userGroupSlugConflictsWith(connection: Connection, rawValue: String): IO[Boolean] =
    UserGroupSlug.parse(rawValue) match
      case Left(_) => IO.pure(false)
      case Right(slug) => ResolveUserGroupSlug.plan(connection, slug).map(_.exists)

  /** 规范化并校验展示名，返回可写入用户资料的值。 */
  private def validateDisplayName(displayName: DisplayName): IO[DisplayName] =
    val normalized = displayName.value.trim

    if normalized.isEmpty then HttpApiError.raise(HttpApiError.badRequest("Display name is required."))
    else if normalized.length > 120 then HttpApiError.raise(HttpApiError.badRequest("Display name must be at most 120 characters."))
    else IO.pure(DisplayName(normalized))

  /** 复用邮箱值对象规则校验注册邮箱，并返回去除首尾空白后的值。 */
  private def validateEmail(email: EmailAddress): IO[EmailAddress] =
    EmailAddress.validationMessage(email) match
      case Some(message) => HttpApiError.raise(HttpApiError.badRequest(message))
      case None => IO.pure(EmailAddress(email.value.trim))
