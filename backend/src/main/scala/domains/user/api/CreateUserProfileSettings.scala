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

/** 内部用户资料创建 API，通常由注册流程在创建账号后调用。 */
object CreateUserProfileSettings extends InternalOnlyApi[CreateUserProfileSettings.Input, UserProfileSettings]:

  /** 创建用户资料所需的完整输入，字段直接映射 user_profiles 表。 */
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

  /** 在当前事务中插入用户资料并返回初始设置，外部 HTTP 不可直接调用。 */
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
