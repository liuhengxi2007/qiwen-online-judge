package domains.user.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.user.objects.Username
import domains.user.objects.UserProfileSettings
import domains.user.table.user_profile.UserProfileTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部用户资料查询 API，供 auth 等领域按用户名读取资料设置。 */
object FindUserProfileSettings extends InternalOnlyApi[Username, Option[UserProfileSettings]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/internal/users/:username/profile-settings")

  /** 在当前事务中查找用户资料设置，不存在时返回 None。 */
  override def plan(connection: Connection, username: Username): IO[Option[UserProfileSettings]] =
    UserProfileTable.findSettingsByUsername(connection, username)
