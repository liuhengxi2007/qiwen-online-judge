package domains.user.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, Username}
import domains.user.objects.UserProfileSettings
import domains.user.table.user_profile.UserProfileTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object CreateUserProfileSettings extends InternalOnlyApi[CreateUserProfileSettings.Input, UserProfileSettings]:

  final case class Input(
    username: Username,
    displayName: DisplayName,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean
  )

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/users/profile-settings")

  override def plan(connection: Connection, input: Input): IO[UserProfileSettings] =
    UserProfileTable.insertProfile(
      connection,
      username = input.username,
      displayName = input.displayName,
      displayMode = input.displayMode,
      locale = input.locale,
      problemTitleDisplayMode = input.problemTitleDisplayMode,
      autoMarkMessageRead = input.autoMarkMessageRead
    )
