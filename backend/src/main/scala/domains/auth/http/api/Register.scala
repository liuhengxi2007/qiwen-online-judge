package domains.auth.http.api

import cats.effect.IO
import domains.auth.application.{PasswordHasher, SessionStore}
import domains.auth.http.{AuthApiSupport, PublicResponseApi}
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.objects.EmailAddress
import domains.auth.objects.request.RegisterRequest
import domains.auth.table.auth_user.AuthUserTable
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, Username}
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.table.user_group.UserGroupTable
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import io.circe.syntax.*

import java.sql.Connection

final case class Register(sessionStore: SessionStore) extends PublicResponseApi[RegisterRequest]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/register")

  override def decode(request: Request[IO], pathParams: PathParams): IO[RegisterRequest] =
    val _ = pathParams
    request.as[RegisterRequest]

  override def plan(connection: Connection, request: RegisterRequest): IO[Response[IO]] =
    for
      username <- HttpApiError.fromEitherBadRequest(Username.parse(request.username.value))
      validRequest = request.copy(username = username)
      existingUser <- AuthUserTable.findByUsername(connection, validRequest.username)
      _ <- HttpApiError.ensure(existingUser.isEmpty, HttpApiError.conflict(ApiMessages.usernameExists))
      conflictingUserGroupSlug <- userGroupSlugConflictsWith(connection, validRequest.username.value)
      _ <- HttpApiError.ensure(!conflictingUserGroupSlug, HttpApiError.conflict(ApiMessages.usernameConflictsWithGroup))
      displayName <- validateDisplayName(validRequest.displayName)
      email <- validateEmail(validRequest.email)
      passwordHash <- PasswordHasher.hashPassword(validRequest.password)
      createdUser <- AuthUserTable.insert(
        connection,
        username = validRequest.username,
        displayName = displayName,
        email = email,
        displayMode = UserDisplayMode.DisplayName,
        locale = UserLocale.En,
        problemTitleDisplayMode = ProblemTitleDisplayMode.Title,
        autoMarkMessageRead = false,
        passwordHash = passwordHash
      )
      sessionToken <- sessionStore.createSessionInConnection(connection, createdUser.username)
    yield
      Response[IO](status = Status.Created)
        .withEntity(AuthApiSupport.toRegisterResponse(createdUser, "Registration successful").asJson)
        .addCookie(AuthApiSupport.sessionCookie(sessionToken))

  private def userGroupSlugConflictsWith(connection: Connection, rawValue: String): IO[Boolean] =
    UserGroupSlug.parse(rawValue) match
      case Left(_) => IO.pure(false)
      case Right(slug) => UserGroupTable.findBySlug(connection, slug).map(_.nonEmpty)

  private def validateDisplayName(displayName: DisplayName): IO[DisplayName] =
    val normalized = displayName.value.trim

    if normalized.isEmpty then HttpApiError.raise(HttpApiError.badRequest("Display name is required."))
    else if normalized.length > 120 then HttpApiError.raise(HttpApiError.badRequest("Display name must be at most 120 characters."))
    else IO.pure(DisplayName(normalized))

  private def validateEmail(email: EmailAddress): IO[EmailAddress] =
    AuthApiSupport.validateEmail(email) match
      case Some(message) => HttpApiError.raise(HttpApiError.badRequest(message))
      case None => IO.pure(EmailAddress(email.value.trim))
