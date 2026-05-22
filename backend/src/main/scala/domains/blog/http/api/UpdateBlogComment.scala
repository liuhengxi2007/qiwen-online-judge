package domains.blog.http.api



import domains.blog.http.*
import domains.blog.http.codec.BlogHttpCodecs.given
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.user.model.Username
import domains.blog.application.BlogCommands
import domains.blog.model.{BlogCommentId, BlogId}
import domains.blog.application.input.{CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.notification.application.NotificationEventHub
import domains.problem.model.ProblemSlug
import shared.http.AuthenticatedHttpExecutor
import shared.model.PageRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateBlogComment:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = BlogHttpPlanDefinitions.plans(notificationEventHub)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "update" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.executeDecoded[UpdateBlogCommentRequest, BlogHttpPlans.UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult](
              request,
              plans.updateBlogComment
            )(updateRequest => BlogHttpPlans.UpdateBlogCommentInput(blogId, commentId, updateRequest))
    }

  private def parsePageRequest(queryParams: Map[String, String]): PageRequest =
    PageRequest(
      page = parsePositiveInt(queryParams.get("page"), 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)
