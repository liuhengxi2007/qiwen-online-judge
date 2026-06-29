package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.user.table.user_profile.UserProfileTable
import domains.user.utils.{UserAvatarStorage, UserAvatarStorageContext}
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 用户头像读取 API，从对象存储读取头像字节并返回原始图片响应。API 对齐例外：头像由 img URL 消费原始字节，不走前端 API 消息包装。 */
final case class GetUserAvatar(userAvatarStorage: UserAvatarStorageContext) extends AuthenticatedResponseApi[Username]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/avatar")

  /** 从路径读取目标用户名；请求体被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  /** 查询头像元数据并读取对象内容；用户或对象缺失时返回 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[Response[IO]] =
    val _ = actor
    UserProfileTable.findAvatarByUsername(connection, targetUsername).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      case Some((objectKey, contentType)) =>
        UserAvatarStorage.readObject(userAvatarStorage, objectKey).flatMap {
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
          case Some(bytes) => IO.pure(avatarResponse(contentType, bytes))
        }
    }

  private def avatarResponse(contentType: String, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), contentType),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])
