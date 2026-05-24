package domains.blog.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.blog.http.api.ListBlogs
import domains.blog.http.api.ListProblemBlogs
import domains.blog.http.api.ListPendingProblemBlogs
import domains.blog.http.api.SubmitBlogToProblem
import domains.blog.http.api.AcceptBlogProblemSubmission
import domains.blog.http.api.LinkBlogToProblem
import domains.blog.http.api.UnlinkBlogFromProblem
import domains.blog.http.api.CreateBlog
import domains.blog.http.api.GetBlog
import domains.blog.http.api.VoteBlog
import domains.blog.http.api.UpdateBlog
import domains.blog.http.api.DeleteBlog
import domains.blog.http.api.CreateBlogComment
import domains.blog.http.api.CreateBlogCommentReply
import domains.blog.http.api.VoteBlogComment
import domains.blog.http.api.UpdateBlogComment
import domains.blog.http.api.DeleteBlogComment
import domains.auth.application.SessionStore
import domains.notification.application.NotificationEventHub
import domains.auth.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object BlogRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = BlogHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      notificationEventHub = notificationEventHub,
      handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore),
      plans = BlogHttpPlanDefinitions.plans(notificationEventHub)
    )

    ListBlogs.routes(context) <+>
      ListProblemBlogs.routes(context) <+>
      ListPendingProblemBlogs.routes(context) <+>
      SubmitBlogToProblem.routes(context) <+>
      AcceptBlogProblemSubmission.routes(context) <+>
      LinkBlogToProblem.routes(context) <+>
      UnlinkBlogFromProblem.routes(context) <+>
      CreateBlog.routes(context) <+>
      GetBlog.routes(context) <+>
      VoteBlog.routes(context) <+>
      UpdateBlog.routes(context) <+>
      DeleteBlog.routes(context) <+>
      CreateBlogComment.routes(context) <+>
      CreateBlogCommentReply.routes(context) <+>
      VoteBlogComment.routes(context) <+>
      UpdateBlogComment.routes(context) <+>
      DeleteBlogComment.routes(context)
