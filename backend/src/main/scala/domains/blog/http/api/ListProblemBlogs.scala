package domains.blog.http.api



import domains.blog.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.Username
import domains.blog.application.BlogCommands
import domains.blog.model.{BlogCommentId, BlogId}
import domains.blog.http.request.{CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.notification.application.NotificationEventHub
import domains.problem.model.ProblemSlug
import domains.shared.http.AuthenticatedHttpExecutor
import domains.shared.model.PageRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListProblemBlogs:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = BlogHttpPlanDefinitions.plans(notificationEventHub)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blogs" =>
        ProblemSlug.parse(rawProblemSlug) match
          case Left(message) =>
            domains.shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(problemSlug) =>
            handlers.execute(request, BlogHttpPlans.ProblemBlogsInput(problemSlug, parsePageRequest(request.uri.query.params)), plans.listProblemBlogs)
    }

  private def parsePageRequest(queryParams: Map[String, String]): PageRequest =
    PageRequest(
      page = parsePositiveInt(queryParams.get("page"), 1),
      pageSize = parsePositiveInt(queryParams.get("pageSize"), 10)
    )

  private def parsePositiveInt(rawValue: Option[String], defaultValue: Int): Int =
    rawValue.flatMap(_.toIntOption).filter(_ > 0).getOrElse(defaultValue)

