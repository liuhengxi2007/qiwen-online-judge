package domains.user.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.blog.table.blog.BlogPostQueryTable

import domains.user.objects.{UserContribution, Username}
import domains.user.objects.response.UserProfileResponse
import domains.user.table.user.UserTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetUserProfile extends AuthenticatedApi[Username, UserProfileResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/users/:targetUsername/profile")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserProfileResponse] = summon[Encoder[UserProfileResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  override def plan(connection: Connection, actor: AuthUser, targetUsername: Username): IO[UserProfileResponse] =
    val _ = actor
    UserTable.findByUsername(connection, targetUsername).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      case Some(targetUser) =>
        for
          contribution <- BlogPostQueryTable.contributionByAuthor(connection, targetUsername)
          acceptedProblems <- UserTable.listAcceptedProblems(connection, targetUsername)
        yield UserProfileResponse(
          username = targetUser.username,
          displayName = targetUser.displayName,
          contribution = UserContribution(contribution),
          acceptedProblems = acceptedProblems
        )
    }
