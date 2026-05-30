package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.user.objects.Username
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

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

  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[UserSettingsResponse] =
    for
      _ <- HttpApiError.ensure(
        targetUsername.value == actor.username.value || actor.siteManager,
        HttpApiError.forbidden(ApiMessages.siteManagerRequired)
      )
      maybeSettings <- UserTable.findUserSettingsByUsername(connection, targetUsername)
      settings <- maybeSettings match
        case Some(settings) => IO.pure(settings)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    yield settings
