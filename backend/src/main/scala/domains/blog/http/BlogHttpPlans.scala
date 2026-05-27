package domains.blog.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.user.objects.Username
import domains.blog.application.BlogCommands
import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.{CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.notification.application.{NotificationEventHub, NotificationStreamEvent}
import domains.problem.objects.ProblemSlug
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import shared.objects.PageRequest

import java.sql.Connection

object BlogHttpPlans:

  final case class ListBlogsInput(authorUsername: Option[Username], pageRequest: PageRequest)

  case object ListBlogs extends PlainAuthenticatedHttpPlan[AuthUser, ListBlogsInput, BlogCommands.ListBlogsResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ListBlogsInput
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listBlogs(databaseSession, actor, input.authorUsername, input.pageRequest)

  final case class ProblemBlogsInput(problemSlug: ProblemSlug, pageRequest: PageRequest)

  case object ListProblemBlogs extends PlainAuthenticatedHttpPlan[AuthUser, ProblemBlogsInput, BlogCommands.ListBlogsResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemBlogsInput
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listProblemBlogs(databaseSession, actor, input.problemSlug, input.pageRequest)

  case object ListPendingProblemBlogs extends PlainAuthenticatedHttpPlan[AuthUser, ProblemBlogsInput, BlogCommands.ListBlogsResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemBlogsInput
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listPendingProblemBlogs(databaseSession, actor, input.problemSlug, input.pageRequest)

  case object CreateBlog extends TransactionAuthenticatedHttpPlan[AuthUser, CreateBlogRequest, BlogCommands.CreateBlogResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateBlogRequest
    ): IO[BlogCommands.CreateBlogResult] =
      BlogCommands.createBlog(connection, actor, input)

  case object GetBlog extends PlainAuthenticatedHttpPlan[AuthUser, BlogId, BlogCommands.GetBlogResult]:

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: BlogId
    ): IO[BlogCommands.GetBlogResult] =
      BlogCommands.getBlog(databaseSession, actor, input)

  final case class VoteBlogInput(blogId: BlogId, request: VoteBlogRequest)

  case object VoteBlog extends TransactionAuthenticatedHttpPlan[AuthUser, VoteBlogInput, BlogCommands.VoteBlogResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: VoteBlogInput
    ): IO[BlogCommands.VoteBlogResult] =
      BlogCommands.voteBlog(connection, actor, input.blogId, input.request)

  final case class UpdateBlogInput(blogId: BlogId, request: UpdateBlogRequest)

  case object UpdateBlog extends TransactionAuthenticatedHttpPlan[AuthUser, UpdateBlogInput, BlogCommands.UpdateBlogResult]:
    override def execute(connection: Connection, actor: AuthUser, input: UpdateBlogInput): IO[BlogCommands.UpdateBlogResult] =
      BlogCommands.updateBlog(connection, actor, input.blogId, input.request)

  case object DeleteBlog extends TransactionAuthenticatedHttpPlan[AuthUser, BlogId, BlogCommands.DeleteBlogResult]:
    override def execute(connection: Connection, actor: AuthUser, input: BlogId): IO[BlogCommands.DeleteBlogResult] =
      BlogCommands.deleteBlog(connection, actor, input)

  final case class BlogProblemLinkInput(problemSlug: ProblemSlug, blogId: BlogId)

  case object SubmitBlogToProblem extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.SubmitBlogToProblemResult]:
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.SubmitBlogToProblemResult] =
      BlogCommands.submitBlogToProblem(connection, actor, input.problemSlug, input.blogId)

  case object LinkBlogToProblem extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.LinkBlogToProblemResult]:
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.LinkBlogToProblemResult] =
      BlogCommands.linkBlogToProblem(connection, actor, input.problemSlug, input.blogId)

  case object AcceptBlogProblemSubmission extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.AcceptBlogProblemSubmissionResult]:
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.AcceptBlogProblemSubmissionResult] =
      BlogCommands.acceptBlogProblemSubmission(connection, actor, input.problemSlug, input.blogId)

  case object UnlinkBlogFromProblem extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.UnlinkBlogFromProblemResult]:
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.UnlinkBlogFromProblemResult] =
      BlogCommands.unlinkBlogFromProblem(connection, actor, input.problemSlug, input.blogId)

  final case class CreateBlogCommentInput(blogId: BlogId, parentCommentId: Option[BlogCommentId], request: CreateBlogCommentRequest)

  final class CreateBlogCommentPlan(notificationEventHub: NotificationEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateBlogCommentInput
    ): IO[BlogCommands.CreateBlogCommentResult] =
      BlogCommands
        .createBlogCommentWithNotificationRecipients(connection, actor, input.blogId, input.parentCommentId, input.request)
        .flatMap { output =>
          output.notificationRecipients
            .foldLeft(IO.unit)((acc, username) =>
              acc *> notificationEventHub.publish(username, NotificationStreamEvent.NotificationsChanged)
            )
            .as(output.result)
      }

  final case class VoteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: VoteBlogCommentRequest)

  case object VoteBlogComment extends TransactionAuthenticatedHttpPlan[AuthUser, VoteBlogCommentInput, BlogCommands.VoteBlogCommentResult]:

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: VoteBlogCommentInput
    ): IO[BlogCommands.VoteBlogCommentResult] =
      BlogCommands.voteBlogComment(connection, actor, input.blogId, input.commentId, input.request)

  final case class UpdateBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest)

  case object UpdateBlogComment extends TransactionAuthenticatedHttpPlan[AuthUser, UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult]:
    override def execute(connection: Connection, actor: AuthUser, input: UpdateBlogCommentInput): IO[BlogCommands.UpdateBlogCommentResult] =
      BlogCommands.updateBlogComment(connection, actor, input.blogId, input.commentId, input.request)

  final case class DeleteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId)

  case object DeleteBlogComment extends TransactionAuthenticatedHttpPlan[AuthUser, DeleteBlogCommentInput, BlogCommands.DeleteBlogCommentResult]:
    override def execute(connection: Connection, actor: AuthUser, input: DeleteBlogCommentInput): IO[BlogCommands.DeleteBlogCommentResult] =
      BlogCommands.deleteBlogComment(connection, actor, input.blogId, input.commentId)
