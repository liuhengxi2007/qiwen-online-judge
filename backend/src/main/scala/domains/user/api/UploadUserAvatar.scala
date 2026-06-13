package domains.user.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user_profile.UserProfileTable
import domains.user.utils.{UserAvatarStorage, UserAvatarUploadValidation}
import io.circe.Encoder
import org.http4s.multipart.Multipart
import org.http4s.{Method, Request, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 上传用户头像 API，校验 multipart 图片并同步更新对象存储和数据库元数据。 */
final case class UploadUserAvatar(userAvatarStorage: UserAvatarStorage)
    extends AuthenticatedApi[(Username, Array[Byte], Option[String]), UserSettingsResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/avatar")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserSettingsResponse] = summon[Encoder[UserSettingsResponse]]

  /** 从路径读取目标用户名，并从 multipart file 字段读取头像字节和 content type。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, Array[Byte], Option[String])] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      targetUsername = Username.canonical(rawUsername)
      multipart <- request.as[Multipart[IO]]
      filePart <- multipart.parts.find(_.name.contains("file")) match
        case Some(part) => IO.pure(part)
        case None => HttpApiError.raise(HttpApiError.badRequest("Multipart file field 'file' is required."))
      /** FIXME-CN: 头像 body 在大小校验前一次性读入内存，超大 multipart 请求可能造成内存压力。 */
      bytes <- filePart.body.compile.to(Array)
      contentType = filePart.headers.headers.find(_.name == CIString("Content-Type")).map(_.value.takeWhile(_ != ';').trim)
    yield (targetUsername, bytes, contentType)

  /** 校验权限、文件大小和类型，先写对象存储再更新数据库，数据库失败会清理新对象。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (Username, Array[Byte], Option[String])
  ): IO[UserSettingsResponse] =
    val (targetUsername, bytes, contentType) = input
    for
      _ <- UserAvatarApiHelpers.ensureCanManageAvatar(actor, targetUsername)
      _ <- UserProfileTable.findSettingsByUsername(connection, targetUsername).flatMap {
        case Some(_) => IO.unit
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
      uploadId <- IO.randomUUID
      prepared <- HttpApiError.fromEitherBadRequest(UserAvatarUploadValidation.prepare(targetUsername, bytes, contentType, uploadId))
      previousAvatar <- UserProfileTable.findAvatarByUsername(connection, targetUsername)
      now <- IO.realTimeInstant
      _ <- userAvatarStorage.writeObject(prepared.objectKey, prepared.bytes, prepared.contentType)
      _ <- UserProfileTable
        .updateAvatar(connection, targetUsername, prepared.objectKey, prepared.contentType, now)
        .handleErrorWith(error => userAvatarStorage.deleteObject(prepared.objectKey) *> IO.raiseError(error))
        .flatMap {
          case Some(_) => IO.unit
          case None =>
            userAvatarStorage.deleteObject(prepared.objectKey) *> HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
        }
      _ <- previousAvatar.traverse(avatar => userAvatarStorage.deleteObject(avatar.objectKey)).void
      settings <- UserAvatarApiHelpers.refreshedSettings(connection, targetUsername)
    yield settings
