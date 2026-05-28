package domains.auth.http

import domains.auth.objects.response.{AuthAccountListItem, LoginResponse, RegisterResponse, SessionResponse}
import domains.auth.objects.{AuthUser, EmailAddress, SessionToken}
import domains.user.objects.UserPreferences
import org.http4s.{ResponseCookie, SameSite}
import shared.http.ApiMessages
import shared.objects.response.SuccessResponse

object AuthApiSupport:

  val protectedAdminUsername: String = "admin"

  private val sessionCookieName = "qiwen_session"

  def sessionCookie(token: SessionToken): ResponseCookie =
    ResponseCookie(
      name = sessionCookieName,
      content = token.value,
      path = Some("/"),
      httpOnly = true,
      sameSite = Some(SameSite.Lax)
    )

  val clearedSessionCookie: ResponseCookie =
    ResponseCookie(
      name = sessionCookieName,
      content = "",
      path = Some("/"),
      httpOnly = true,
      sameSite = Some(SameSite.Lax),
      maxAge = Some(0L)
    )

  def toSessionResponse(user: AuthUser): SessionResponse =
    SessionResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = UserPreferences(
        displayMode = user.displayMode,
        locale = user.locale,
        problemTitleDisplayMode = user.problemTitleDisplayMode,
        autoMarkMessageRead = user.autoMarkMessageRead
      ),
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )

  def toLoginResponse(user: AuthUser, message: String): LoginResponse =
    LoginResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = toSessionResponse(user).preferences,
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )

  def toRegisterResponse(user: AuthUser, message: String): RegisterResponse =
    RegisterResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = toSessionResponse(user).preferences,
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )

  def toAuthAccountListItem(user: AuthUser): AuthAccountListItem =
    AuthAccountListItem(
      username = user.username,
      displayName = user.displayName,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )

  def validateEmail(email: EmailAddress): Option[String] =
    val normalized = email.value.trim
    val emailPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".r

    if normalized.isEmpty then Some("Email is required.")
    else if normalized.length > 255 then Some("Email must be at most 255 characters.")
    else if emailPattern.matches(normalized) then None
    else Some("Please enter a valid email address.")

  def success(apiMessage: shared.http.ApiMessage): SuccessResponse =
    SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)

  def loggedOutSuccess: SuccessResponse =
    success(ApiMessages.loggedOut)
