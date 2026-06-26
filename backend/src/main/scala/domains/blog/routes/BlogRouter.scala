package domains.blog.routes

import cats.effect.IO
import database.DatabaseSession
import domains.auth.api.SessionStoreContext
import domains.auth.api.{ApiObjectContext, ApiObjectRouter}
import domains.blog.api.*
import domains.notification.api.NotificationEventHubContext
import org.http4s.HttpRoutes

/** 汇总博客 domain 的 http4s 路由，负责把数据库会话、登录会话和通知事件中心注入 API 对象。 */
object BlogRouter:

  /** 构造博客相关 HTTP 路由，评论 API 会使用 notificationEventHub 推送通知变更。 */
  def routes(databaseSession: DatabaseSession, sessionStore: SessionStoreContext, notificationEventHub: NotificationEventHubContext): HttpRoutes[IO] =
    val context = ApiObjectContext(databaseSession, sessionStore)

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
        DeleteBlogComment,
        GetBlogContributionForAuthor
      )
    )
