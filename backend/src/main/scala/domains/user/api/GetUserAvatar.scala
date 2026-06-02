package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedResponseApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.user.table.user_profile.UserProfileTable
import domains.user.utils.UserAvatarStorage
import fs2.Stream
import org.http4s.{Header, Method, Request, Response, Status}
import org.typelevel.ci.CIString
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class GetUserAvatar(userAvatarStorage: UserAvatarStorage) extends AuthenticatedResponseApi[Username]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/avatar")

  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[Response[IO]] =
    val _ = actor
    UserProfileTable.findAvatarByUsername(connection, targetUsername).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      case Some(avatar) =>
        userAvatarStorage.readObject(avatar.objectKey).flatMap {
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
          case Some(bytes) => IO.pure(avatarResponse(avatar.contentType, bytes))
        }
    }

  private def avatarResponse(contentType: String, bytes: Array[Byte]): Response[IO] =
    Response[IO](status = Status.Ok)
      .putHeaders(
        Header.Raw(CIString("Content-Type"), contentType),
        Header.Raw(CIString("Content-Length"), bytes.length.toString)
      )
      .withBodyStream(Stream.emits(bytes).covary[IO])
