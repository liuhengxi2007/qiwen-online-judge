package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.api.GetBlogContributionForAuthor
import domains.rating.api.GetUserRating
import domains.user.objects.{UserContribution, Username}
import domains.user.objects.response.UserProfileResponse
import domains.user.table.user_profile.{UserProfileQueryTable, UserProfileTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 用户公开资料 API，返回资料、贡献、rating 和通过题目列表。 */
object GetUserProfile extends AuthenticatedApi[Username, UserProfileResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/profile")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserProfileResponse] = summon[Encoder[UserProfileResponse]]

  /** 从路径读取目标用户名；请求体被忽略。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  /** 聚合用户资料、博客贡献、rating 和 AC 题目；目标用户不存在返回 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, targetUsername: Username): IO[UserProfileResponse] =
    val _ = actor
    UserProfileTable.findSettingsByUsername(connection, targetUsername).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      case Some(targetProfile) =>
        for
          contribution <- GetBlogContributionForAuthor.plan(connection, targetUsername).map(_.contribution)
          rating <- GetUserRating.plan(connection, targetUsername)
          acceptedProblems <- UserProfileQueryTable.listAcceptedProblems(connection, targetUsername)
        yield UserProfileResponse(
          username = targetProfile.username,
          displayName = targetProfile.displayName,
          avatarUrl = targetProfile.avatarUrl,
          contribution = UserContribution(BigDecimal(contribution)),
          rating = rating,
          acceptedProblems = acceptedProblems
        )
    }
