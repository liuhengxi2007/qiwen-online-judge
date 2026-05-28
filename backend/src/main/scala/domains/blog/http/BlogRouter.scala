package domains.blog.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.utils.SessionStore
import domains.auth.http.{ApiObjectContext, ApiObjectRouter, SessionResolver}
import domains.blog.http.api.*
import domains.notification.utils.NotificationEventHub
import org.http4s.HttpRoutes

object BlogRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, SessionResolver(sessionStore))

    ApiObjectRouter.routes(
      context,
      List(
        ListBlogs,
        ListProblemBlogs,
        ListPendingProblemBlogs,
        SubmitBlogToProblem,
        AcceptBlogProblemSubmission,
        LinkBlogToProblem,
        UnlinkBlogFromProblem,
        CreateBlog,
        GetBlog,
        VoteBlog,
        UpdateBlog,
        DeleteBlog,
        CreateBlogComment(notificationEventHub),
        CreateBlogCommentReply(notificationEventHub),
        VoteBlogComment,
        UpdateBlogComment,
        DeleteBlogComment
      )
    )
