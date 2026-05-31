package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.user.objects.Username
import domains.user.objects.request.UpdateOwnProfileRequest
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user_profile.UserProfileTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateUserProfile extends AuthenticatedApi[(Username, UpdateOwnProfileRequest), UserSettingsResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/settings/profile")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserSettingsResponse] = summon[Encoder[UserSettingsResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, UpdateOwnProfileRequest)] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      updateRequest <- request.as[UpdateOwnProfileRequest]
    yield (Username.canonical(rawUsername), updateRequest)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (Username, UpdateOwnProfileRequest)
  ): IO[UserSettingsResponse] =
    val (targetUsername, request) = input
    for
      _ <- HttpApiError.ensure(
        targetUsername.value == actor.username.value || actor.siteManager,
        HttpApiError.forbidden(ApiMessages.siteManagerRequired)
      )
      maybeTargetProfile <- UserProfileTable.findSettingsByUsername(connection, targetUsername)
      targetProfile <- maybeTargetProfile match
        case Some(profile) => IO.pure(profile)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      updated <- UserProfileTable.updateSettings(
        connection,
        targetProfile.username,
        displayName = request.displayName,
        displayMode = targetProfile.displayMode,
        locale = targetProfile.locale,
        problemTitleDisplayMode = targetProfile.problemTitleDisplayMode,
        autoMarkMessageRead = targetProfile.autoMarkMessageRead
      )
      _ <- updated match
        case Some(_) => IO.unit
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      settings <- UserProfileTable.findUserSettingsByUsername(connection, targetUsername).flatMap {
        case Some(settings) => IO.pure(settings)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
    yield settings
