package domains.user.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.user.http.UserApiSupport
import domains.user.http.codec.UserHttpCodecs.given
import domains.user.objects.Username
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetUserSettings extends AuthenticatedApi[Username, UserSettingsResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/settings")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserSettingsResponse] = summon[Encoder[UserSettingsResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  override def plan(connection: Connection, actor: AuthUser, targetUsername: Username): IO[UserSettingsResponse] =
    for
      _ <- HttpApiError.ensure(
        targetUsername.value == actor.username.value || actor.siteManager,
        HttpApiError.forbidden(ApiMessages.siteManagerRequired)
      )
      maybeUser <- UserTable.findByUsername(connection, targetUsername)
      targetUser <- maybeUser match
        case Some(user) => IO.pure(user)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    yield UserApiSupport.toUserSettingsResponse(targetUser)
