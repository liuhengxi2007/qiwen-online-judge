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
          plans("ListBlogs").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.Plain[Option[Username], BlogCommands.ListBlogsResult]]
        )

      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blogs" =>
        ProblemSlug.parse(rawProblemSlug) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(problemSlug) =>
            handlers.execute(request, problemSlug, plans("ListProblemBlogs").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.Plain[ProblemSlug, BlogCommands.ListBlogsResult]])

      case request @ GET -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" =>
        ProblemSlug.parse(rawProblemSlug) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(problemSlug) =>
            handlers.execute(request, problemSlug, plans("ListPendingProblemBlogs").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.Plain[ProblemSlug, BlogCommands.ListBlogsResult]])

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" / rawBlogId =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans("SubmitBlogToProblem").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.BlogProblemLinkInput, BlogCommands.SubmitBlogToProblemResult]])

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-submissions" / rawBlogId / "accept" =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans("AcceptBlogProblemSubmission").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.BlogProblemLinkInput, BlogCommands.AcceptBlogProblemSubmissionResult]])

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-links" / rawBlogId =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans("LinkBlogToProblem").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.BlogProblemLinkInput, BlogCommands.LinkBlogToProblemResult]])

      case request @ POST -> Root / "api" / "problems" / rawProblemSlug / "blog-links" / rawBlogId / "delete" =>
        (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
          case (Left(message), _) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(problemSlug), Right(blogId)) =>
            handlers.execute(request, BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId), plans("UnlinkBlogFromProblem").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.BlogProblemLinkInput, BlogCommands.UnlinkBlogFromProblemResult]])

      case request @ POST -> Root / "api" / "blogs" =>
        handlers.executeDecoded[CreateBlogRequest, CreateBlogRequest, BlogCommands.CreateBlogResult](
          request,
          plans("CreateBlog").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[CreateBlogRequest, BlogCommands.CreateBlogResult]]
        )(identity)

      case request @ GET -> Root / "api" / "blogs" / rawBlogId =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.execute(request, blogId, plans("GetBlog").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.Plain[BlogId, BlogCommands.GetBlogResult]])

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "vote" =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.executeDecoded[VoteBlogRequest, BlogHttpPlans.VoteBlogInput, BlogCommands.VoteBlogResult](
              request,
              plans("VoteBlog").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.VoteBlogInput, BlogCommands.VoteBlogResult]]
            )(voteRequest => BlogHttpPlans.VoteBlogInput(blogId, voteRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "update" =>
        BlogId.parse(rawBlogId) match
          case Left(message) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.executeDecoded[UpdateBlogRequest, BlogHttpPlans.UpdateBlogInput, BlogCommands.UpdateBlogResult](
              request,
              plans("UpdateBlog").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.UpdateBlogInput, BlogCommands.UpdateBlogResult]]
            )(updateRequest => BlogHttpPlans.UpdateBlogInput(blogId, updateRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "delete" =>
        BlogId.parse(rawBlogId) match
          case Left(message) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.execute(request, blogId, plans("DeleteBlog").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogId, BlogCommands.DeleteBlogResult]])

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" =>
        BlogId.parse(rawBlogId) match
          case Left(message) =>
            domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case Right(blogId) =>
            handlers.executeDecoded[CreateBlogCommentRequest, BlogHttpPlans.CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult](
              request,
              plans("CreateBlogComment").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult]]
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
              plans("CreateBlogComment").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult]]
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
              plans("VoteBlogComment").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.VoteBlogCommentInput, BlogCommands.VoteBlogCommentResult]]
            )(voteRequest => BlogHttpPlans.VoteBlogCommentInput(blogId, commentId, voteRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "update" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.executeDecoded[UpdateBlogCommentRequest, BlogHttpPlans.UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult](
              request,
              plans("UpdateBlogComment").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult]]
            )(updateRequest => BlogHttpPlans.UpdateBlogCommentInput(blogId, commentId, updateRequest))

      case request @ POST -> Root / "api" / "blogs" / rawBlogId / "comments" / rawCommentId / "delete" =>
        (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
          case (Left(message), _) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (_, Left(message)) => domains.shared.http.HttpResponseSupport.validationErrorResponse(message)
          case (Right(blogId), Right(commentId)) =>
            handlers.execute(request, BlogHttpPlans.DeleteBlogCommentInput(blogId, commentId), plans("DeleteBlogComment").asInstanceOf[domains.shared.http.AuthenticatedHttpPlanRegistry.RegisteredPlan.WithTransaction[BlogHttpPlans.DeleteBlogCommentInput, BlogCommands.DeleteBlogCommentResult]])
    }
