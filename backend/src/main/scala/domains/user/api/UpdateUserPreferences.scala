package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.user.objects.Username
import domains.user.objects.request.UpdateOwnPreferencesRequest
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateUserPreferences extends AuthenticatedApi[(Username, UpdateOwnPreferencesRequest), UserSettingsResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/settings/preferences")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserSettingsResponse] = summon[Encoder[UserSettingsResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, UpdateOwnPreferencesRequest)] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      updateRequest <- request.as[UpdateOwnPreferencesRequest]
    yield (Username.canonical(rawUsername), updateRequest)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (Username, UpdateOwnPreferencesRequest)
  ): IO[UserSettingsResponse] =
    val (targetUsername, request) = input
    for
      _ <- HttpApiError.ensure(
        targetUsername.value == actor.username.value || actor.siteManager,
        HttpApiError.forbidden(ApiMessages.siteManagerRequired)
      )
      maybeTargetProfile <- UserTable.findSettingsByUsername(connection, targetUsername)
      targetProfile <- maybeTargetProfile match
        case Some(profile) => IO.pure(profile)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      updated <- UserTable.updateSettings(
        connection,
        targetProfile.username,
        displayName = targetProfile.displayName,
        displayMode = request.preferences.displayMode,
        locale = request.preferences.locale,
        problemTitleDisplayMode = request.preferences.problemTitleDisplayMode,
        autoMarkMessageRead = request.preferences.autoMarkMessageRead
      )
      _ <- updated match
        case Some(_) => IO.unit
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      settings <- UserTable.findUserSettingsByUsername(connection, targetUsername).flatMap {
        case Some(settings) => IO.pure(settings)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
    yield settings
