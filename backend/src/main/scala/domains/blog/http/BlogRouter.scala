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
import org.http4s.HttpRoutes

object BlogRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    ListBlogs.routes(databaseSession, sessionStore, notificationEventHub) <+>
      ListProblemBlogs.routes(databaseSession, sessionStore, notificationEventHub) <+>
      ListPendingProblemBlogs.routes(databaseSession, sessionStore, notificationEventHub) <+>
      SubmitBlogToProblem.routes(databaseSession, sessionStore, notificationEventHub) <+>
      AcceptBlogProblemSubmission.routes(databaseSession, sessionStore, notificationEventHub) <+>
      LinkBlogToProblem.routes(databaseSession, sessionStore, notificationEventHub) <+>
      UnlinkBlogFromProblem.routes(databaseSession, sessionStore, notificationEventHub) <+>
      CreateBlog.routes(databaseSession, sessionStore, notificationEventHub) <+>
      GetBlog.routes(databaseSession, sessionStore, notificationEventHub) <+>
      VoteBlog.routes(databaseSession, sessionStore, notificationEventHub) <+>
      UpdateBlog.routes(databaseSession, sessionStore, notificationEventHub) <+>
      DeleteBlog.routes(databaseSession, sessionStore, notificationEventHub) <+>
      CreateBlogComment.routes(databaseSession, sessionStore, notificationEventHub) <+>
      CreateBlogCommentReply.routes(databaseSession, sessionStore, notificationEventHub) <+>
      VoteBlogComment.routes(databaseSession, sessionStore, notificationEventHub) <+>
      UpdateBlogComment.routes(databaseSession, sessionStore, notificationEventHub) <+>
      DeleteBlogComment.routes(databaseSession, sessionStore, notificationEventHub)
