package domains.blog.http



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.user.model.Username
import domains.blog.application.BlogCommands
import domains.blog.model.{BlogCommentId, BlogId}
import domains.blog.model.request.{CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.notification.application.{NotificationEventHub, NotificationStreamEvent}
import domains.problem.model.ProblemSlug
import shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}
import shared.model.PageRequest

import java.sql.Connection

object BlogHttpPlans:

  final case class ListBlogsInput(authorUsername: Option[Username], pageRequest: PageRequest)

  case object ListBlogs extends PlainAuthenticatedHttpPlan[AuthUser, ListBlogsInput, BlogCommands.ListBlogsResult]:

    override val name: String = "ListBlogs"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ListBlogsInput
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listBlogs(databaseSession, actor, input.authorUsername, input.pageRequest)

  final case class ProblemBlogsInput(problemSlug: ProblemSlug, pageRequest: PageRequest)

  case object ListProblemBlogs extends PlainAuthenticatedHttpPlan[AuthUser, ProblemBlogsInput, BlogCommands.ListBlogsResult]:

    override val name: String = "ListProblemBlogs"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemBlogsInput
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listProblemBlogs(databaseSession, actor, input.problemSlug, input.pageRequest)

  case object ListPendingProblemBlogs extends PlainAuthenticatedHttpPlan[AuthUser, ProblemBlogsInput, BlogCommands.ListBlogsResult]:

    override val name: String = "ListPendingProblemBlogs"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemBlogsInput
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listPendingProblemBlogs(databaseSession, actor, input.problemSlug, input.pageRequest)

  case object CreateBlog extends TransactionAuthenticatedHttpPlan[AuthUser, CreateBlogRequest, BlogCommands.CreateBlogResult]:

    override val name: String = "CreateBlog"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateBlogRequest
    ): IO[BlogCommands.CreateBlogResult] =
      BlogCommands.createBlog(connection, actor, input)

  case object GetBlog extends PlainAuthenticatedHttpPlan[AuthUser, BlogId, BlogCommands.GetBlogResult]:

    override val name: String = "GetBlog"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: BlogId
    ): IO[BlogCommands.GetBlogResult] =
      BlogCommands.getBlog(databaseSession, actor, input)

  final case class VoteBlogInput(blogId: BlogId, request: VoteBlogRequest)

  case object VoteBlog extends TransactionAuthenticatedHttpPlan[AuthUser, VoteBlogInput, BlogCommands.VoteBlogResult]:

    override val name: String = "VoteBlog"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: VoteBlogInput
    ): IO[BlogCommands.VoteBlogResult] =
      BlogCommands.voteBlog(connection, actor, input.blogId, input.request)

  final case class UpdateBlogInput(blogId: BlogId, request: UpdateBlogRequest)

  case object UpdateBlog extends TransactionAuthenticatedHttpPlan[AuthUser, UpdateBlogInput, BlogCommands.UpdateBlogResult]:
    override val name: String = "UpdateBlog"
    override def execute(connection: Connection, actor: AuthUser, input: UpdateBlogInput): IO[BlogCommands.UpdateBlogResult] =
      BlogCommands.updateBlog(connection, actor, input.blogId, input.request)

  case object DeleteBlog extends TransactionAuthenticatedHttpPlan[AuthUser, BlogId, BlogCommands.DeleteBlogResult]:
    override val name: String = "DeleteBlog"
    override def execute(connection: Connection, actor: AuthUser, input: BlogId): IO[BlogCommands.DeleteBlogResult] =
      BlogCommands.deleteBlog(connection, actor, input)

  final case class BlogProblemLinkInput(problemSlug: ProblemSlug, blogId: BlogId)

  case object SubmitBlogToProblem extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.SubmitBlogToProblemResult]:
    override val name: String = "SubmitBlogToProblem"
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.SubmitBlogToProblemResult] =
      BlogCommands.submitBlogToProblem(connection, actor, input.problemSlug, input.blogId)

  case object LinkBlogToProblem extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.LinkBlogToProblemResult]:
    override val name: String = "LinkBlogToProblem"
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.LinkBlogToProblemResult] =
      BlogCommands.linkBlogToProblem(connection, actor, input.problemSlug, input.blogId)

  case object AcceptBlogProblemSubmission extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.AcceptBlogProblemSubmissionResult]:
    override val name: String = "AcceptBlogProblemSubmission"
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.AcceptBlogProblemSubmissionResult] =
      BlogCommands.acceptBlogProblemSubmission(connection, actor, input.problemSlug, input.blogId)

  case object UnlinkBlogFromProblem extends TransactionAuthenticatedHttpPlan[AuthUser, BlogProblemLinkInput, BlogCommands.UnlinkBlogFromProblemResult]:
    override val name: String = "UnlinkBlogFromProblem"
    override def execute(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[BlogCommands.UnlinkBlogFromProblemResult] =
      BlogCommands.unlinkBlogFromProblem(connection, actor, input.problemSlug, input.blogId)

  final case class CreateBlogCommentInput(blogId: BlogId, parentCommentId: Option[BlogCommentId], request: CreateBlogCommentRequest)

  final class CreateBlogCommentPlan(notificationEventHub: NotificationEventHub)
      extends TransactionAuthenticatedHttpPlan[AuthUser, CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult]:

    override val name: String = "CreateBlogComment"

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

    override val name: String = "VoteBlogComment"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: VoteBlogCommentInput
    ): IO[BlogCommands.VoteBlogCommentResult] =
      BlogCommands.voteBlogComment(connection, actor, input.blogId, input.commentId, input.request)

  final case class UpdateBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest)

  case object UpdateBlogComment extends TransactionAuthenticatedHttpPlan[AuthUser, UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult]:
    override val name: String = "UpdateBlogComment"
    override def execute(connection: Connection, actor: AuthUser, input: UpdateBlogCommentInput): IO[BlogCommands.UpdateBlogCommentResult] =
      BlogCommands.updateBlogComment(connection, actor, input.blogId, input.commentId, input.request)

  final case class DeleteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId)

  case object DeleteBlogComment extends TransactionAuthenticatedHttpPlan[AuthUser, DeleteBlogCommentInput, BlogCommands.DeleteBlogCommentResult]:
    override val name: String = "DeleteBlogComment"
    override def execute(connection: Connection, actor: AuthUser, input: DeleteBlogCommentInput): IO[BlogCommands.DeleteBlogCommentResult] =
      BlogCommands.deleteBlogComment(connection, actor, input.blogId, input.commentId)
