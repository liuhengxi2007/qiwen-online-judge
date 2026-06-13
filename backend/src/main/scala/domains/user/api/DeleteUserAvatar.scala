package domains.user.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user_profile.UserProfileTable
import domains.user.utils.UserAvatarStorage
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 删除用户头像 API，清除数据库头像元数据并删除旧对象存储文件。 */
final case class DeleteUserAvatar(userAvatarStorage: UserAvatarStorage)
    extends AuthenticatedApi[Username, UserSettingsResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/avatar/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserSettingsResponse] = summon[Encoder[UserSettingsResponse]]

  /** 从路径读取目标用户名；请求体被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  /** 校验头像管理权限，清除头像记录并尽力删除旧对象，返回刷新后的设置。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[UserSettingsResponse] =
    for
      _ <- UserAvatarApiHelpers.ensureCanManageAvatar(actor, targetUsername)
      previousAvatar <- UserProfileTable.findAvatarByUsername(connection, targetUsername)
      _ <- UserProfileTable.clearAvatar(connection, targetUsername).flatMap { cleared =>
        HttpApiError.ensure(cleared, HttpApiError.notFound(ApiMessages.userNotFound))
      }
      _ <- previousAvatar.traverse(avatar => userAvatarStorage.deleteObject(avatar.objectKey)).void
      settings <- UserAvatarApiHelpers.refreshedSettings(connection, targetUsername)
    yield settings
