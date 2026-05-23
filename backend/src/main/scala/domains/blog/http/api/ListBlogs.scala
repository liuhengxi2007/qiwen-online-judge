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
import shared.http.utils.PageRequestQuerySupport
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListBlogs:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = BlogHttpPlanDefinitions.plans(notificationEventHub)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "blogs" =>
        handlers.execute(
          request,
          BlogHttpPlans.ListBlogsInput(
            request.uri.query.params.get("username").map(Username.canonical),
            PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
          ),
          plans.listBlogs
        )
    }