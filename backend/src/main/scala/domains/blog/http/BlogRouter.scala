package domains.blog.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.Username
import domains.blog.application.BlogCommands
import domains.blog.model.{BlogCommentId, BlogId, CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.notification.application.NotificationEventHub
import domains.problem.model.ProblemSlug
import domains.shared.http.AuthenticatedHttpExecutor
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object BlogRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore, notificationEventHub: NotificationEventHub): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthenticatedHttpExecutor(databaseSession, sessionStore)
    val plans = BlogHttpPlanDefinitions.plans(notificationEventHub)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "blogs" =>
        handlers.execute(
          request,
          request.uri.query.params.get("username").map(Username.canonical),
          plans.listBlogs
        )

      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blogs" =>
        ProblemSlug.parse(rawProblemSlug) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(problemSlug) =>
            handlers.execute(request, problemSlug, plans.listProblemBlogs)

      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" =>
        ProblemSlug.parse(rawProblemSlug) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(problemSlug) =>
            handlers.execute(request, problemSlug, plans.listPendingProblemBlogs)

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" / rawBlogId =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans.submitBlogToProblem)

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" / rawBlogId / "accept" =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans.acceptBlogProblemSubmission)

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-links" / rawBlogId =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans.linkBlogToProblem)

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-links" / rawBlogId / "delete" =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans.unlinkBlogFromProblem)

      case request @ POST -> Root / "api" / "blogs" =>
        handlers.executeDecoded[CreateBlogRequest, CreateBlogRequest, BlogCommands.CreateBlogResult](
          request,
          plans.createBlog
        )(identity)

      case request @ GET -> Root / "api" / "blogs" / rawBlogId =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.execute(request, blogId, plans.getBlog)

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "vote" =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.executeDecoded[VoteBlogRequest, BlogHttpPlans.VoteBlogInput, BlogCommands.VoteBlogResult](
              request,
              plans.voteBlog
            )(voteRequest => BlogHttpPlans.VoteBlogInput(blogId, voteRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "update" =>
        BlogId.parse(rawBlogId) match
          case Left(message) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.executeDecoded[UpdateBlogRequest, BlogHttpPlans.UpdateBlogInput, BlogCommands.UpdateBlogResult](
              request,
              plans.updateBlog
            )(updateRequest => BlogHttpPlans.UpdateBlogInput(blogId, updateRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "delete" =>
        BlogId.parse(rawBlogId) match
          case Left(message) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.execute(request, blogId, plans.deleteBlog)

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.executeDecoded[CreateBlogCommentRequest, BlogHttpPlans.CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult](
              request,
              plans.createBlogComment
            )(commentRequest => BlogHttpPlans.CreateBlogCommentInput(blogId, None, commentRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "replies" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.executeDecoded[CreateBlogCommentRequest, BlogHttpPlans.CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult](
              request,
              plans.createBlogComment
            )(commentRequest => BlogHttpPlans.CreateBlogCommentInput(blogId, Some(commentId), commentRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "vote" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.executeDecoded[VoteBlogCommentRequest, BlogHttpPlans.VoteBlogCommentInput, BlogCommands.VoteBlogCommentResult](
              request,
              plans.voteBlogComment
            )(voteRequest => BlogHttpPlans.VoteBlogCommentInput(blogId, commentId, voteRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "update" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.executeDecoded[UpdateBlogCommentRequest, BlogHttpPlans.UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult](
              request,
              plans.updateBlogComment
            )(updateRequest => BlogHttpPlans.UpdateBlogCommentInput(blogId, commentId, updateRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "delete" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.execute(request, BlogHttpPlans.DeleteBlogCommentInput(blogId, commentId), plans.deleteBlogComment)
    }
