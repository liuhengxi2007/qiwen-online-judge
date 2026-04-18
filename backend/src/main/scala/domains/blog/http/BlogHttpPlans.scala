package domains.blog.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.blog.application.BlogCommands
import domains.blog.model.{BlogCommentId, BlogId, CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.problem.model.ProblemSlug
import domains.shared.http.{PlainAuthenticatedHttpPlan, TransactionAuthenticatedHttpPlan}

import java.sql.Connection

object BlogHttpPlans:

  case object ListBlogs extends PlainAuthenticatedHttpPlan[Option[Username], BlogCommands.ListBlogsResult]:

    override val name: String = "ListBlogs"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: Option[Username]
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listBlogs(databaseSession, actor, input)

  case object ListProblemBlogs extends PlainAuthenticatedHttpPlan[ProblemSlug, BlogCommands.ListBlogsResult]:

    override val name: String = "ListProblemBlogs"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: ProblemSlug
    ): IO[BlogCommands.ListBlogsResult] =
      BlogCommands.listProblemBlogs(databaseSession, actor, input)

  case object CreateBlog extends TransactionAuthenticatedHttpPlan[CreateBlogRequest, BlogCommands.CreateBlogResult]:

    override val name: String = "CreateBlog"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateBlogRequest
    ): IO[BlogCommands.CreateBlogResult] =
      BlogCommands.createBlog(connection, actor, input)

  case object GetBlog extends PlainAuthenticatedHttpPlan[BlogId, BlogCommands.GetBlogResult]:

    override val name: String = "GetBlog"

    override def execute(
      databaseSession: DatabaseSession,
      actor: AuthUser,
      input: BlogId
    ): IO[BlogCommands.GetBlogResult] =
      BlogCommands.getBlog(databaseSession, actor, input)

  final case class VoteBlogInput(blogId: BlogId, request: VoteBlogRequest)

  case object VoteBlog extends TransactionAuthenticatedHttpPlan[VoteBlogInput, BlogCommands.VoteBlogResult]:

    override val name: String = "VoteBlog"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: VoteBlogInput
    ): IO[BlogCommands.VoteBlogResult] =
      BlogCommands.voteBlog(connection, actor, input.blogId, input.request)

  final case class UpdateBlogInput(blogId: BlogId, request: UpdateBlogRequest)

  case object UpdateBlog extends TransactionAuthenticatedHttpPlan[UpdateBlogInput, BlogCommands.UpdateBlogResult]:
    override val name: String = "UpdateBlog"
    override def execute(connection: Connection, actor: AuthUser, input: UpdateBlogInput): IO[BlogCommands.UpdateBlogResult] =
      BlogCommands.updateBlog(connection, actor, input.blogId, input.request)

  case object DeleteBlog extends TransactionAuthenticatedHttpPlan[BlogId, BlogCommands.DeleteBlogResult]:
    override val name: String = "DeleteBlog"
    override def execute(connection: Connection, actor: AuthUser, input: BlogId): IO[BlogCommands.DeleteBlogResult] =
      BlogCommands.deleteBlog(connection, actor, input)

  final case class CreateBlogCommentInput(blogId: BlogId, parentCommentId: Option[BlogCommentId], request: CreateBlogCommentRequest)

  case object CreateBlogComment extends TransactionAuthenticatedHttpPlan[CreateBlogCommentInput, BlogCommands.CreateBlogCommentResult]:

    override val name: String = "CreateBlogComment"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: CreateBlogCommentInput
    ): IO[BlogCommands.CreateBlogCommentResult] =
      BlogCommands.createBlogComment(connection, actor, input.blogId, input.parentCommentId, input.request)

  final case class VoteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: VoteBlogCommentRequest)

  case object VoteBlogComment extends TransactionAuthenticatedHttpPlan[VoteBlogCommentInput, BlogCommands.VoteBlogCommentResult]:

    override val name: String = "VoteBlogComment"

    override def execute(
      connection: Connection,
      actor: AuthUser,
      input: VoteBlogCommentInput
    ): IO[BlogCommands.VoteBlogCommentResult] =
      BlogCommands.voteBlogComment(connection, actor, input.blogId, input.commentId, input.request)

  final case class UpdateBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest)

  case object UpdateBlogComment extends TransactionAuthenticatedHttpPlan[UpdateBlogCommentInput, BlogCommands.UpdateBlogCommentResult]:
    override val name: String = "UpdateBlogComment"
    override def execute(connection: Connection, actor: AuthUser, input: UpdateBlogCommentInput): IO[BlogCommands.UpdateBlogCommentResult] =
      BlogCommands.updateBlogComment(connection, actor, input.blogId, input.commentId, input.request)

  final case class DeleteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId)

  case object DeleteBlogComment extends TransactionAuthenticatedHttpPlan[DeleteBlogCommentInput, BlogCommands.DeleteBlogCommentResult]:
    override val name: String = "DeleteBlogComment"
    override def execute(connection: Connection, actor: AuthUser, input: DeleteBlogCommentInput): IO[BlogCommands.DeleteBlogCommentResult] =
      BlogCommands.deleteBlogComment(connection, actor, input.blogId, input.commentId)
