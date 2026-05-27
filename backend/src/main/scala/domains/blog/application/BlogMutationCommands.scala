package domains.blog.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.blog.application.BlogCommandResults.*
import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.{CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.blog.table.blog.{BlogCommentTable, BlogCommentVoteTable, BlogPostMutationTable, BlogProblemLinkMutationTable, BlogVoteTable}
import domains.notification.application.NotificationCommands
import domains.problem.application.ProblemCommands
import domains.problem.objects.ProblemSlug
import domains.user.objects.Username

import java.sql.Connection

object BlogMutationCommands:

  final case class CreateBlogCommentWithNotificationRecipientsResult(
    result: CreateBlogCommentResult,
    notificationRecipients: List[Username]
  )

  def createBlog(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    request: CreateBlogRequest
  ): IO[CreateBlogResult] =
    databaseSession.withTransactionConnection(connection => createBlog(connection, actor, request))

  def createBlog(
    connection: Connection,
    actor: AuthUser,
    request: CreateBlogRequest
  ): IO[CreateBlogResult] =
    BlogValidation.validateCreate(request) match
      case Left(message) =>
        IO.pure(CreateBlogResult.ValidationFailed(message))
      case Right(validRequest) =>
        BlogPostMutationTable
          .insert(
            connection,
            actor.username,
            validRequest.title,
            validRequest.content,
            validRequest.visibility
          )
          .map(blog => CreateBlogResult.Created(blog))

  def voteBlog(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    request: VoteBlogRequest
  ): IO[VoteBlogResult] =
    BlogVoteTable.vote(connection, blogId, actor.username, request.vote).map {
      case Some(blog) => VoteBlogResult.Voted(blog)
      case None => VoteBlogResult.NotFound
    }

  def updateBlog(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    request: UpdateBlogRequest
  ): IO[UpdateBlogResult] =
    BlogValidation.validateUpdate(request) match
      case Left(message) =>
        IO.pure(UpdateBlogResult.ValidationFailed(message))
      case Right(validRequest) =>
        BlogPostMutationTable
          .update(
            connection,
            blogId,
            actor.username,
            validRequest.title,
            validRequest.content,
            validRequest.visibility
          )
          .map {
            case Some(blog) => UpdateBlogResult.Updated(blog)
            case None => UpdateBlogResult.NotFound
          }

  def deleteBlog(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId
  ): IO[DeleteBlogResult] =
    BlogPostMutationTable.delete(connection, blogId, actor.username).map {
      case true => DeleteBlogResult.Deleted
      case false => DeleteBlogResult.NotFound
    }

  def submitBlogToProblem(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[SubmitBlogToProblemResult] =
    BlogProblemLinkMutationTable.submitProblem(connection, problemSlug, blogId, actor.username).map {
      case true => SubmitBlogToProblemResult.Submitted
      case false => SubmitBlogToProblemResult.NotFound
    }

  def createBlogComment(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    parentCommentId: Option[BlogCommentId],
    request: CreateBlogCommentRequest
  ): IO[CreateBlogCommentResult] =
    BlogCommentTable.insertComment(connection, blogId, parentCommentId, actor.username, request.content).map {
      case Some((blog, createdCommentId)) => CreateBlogCommentResult.Created(blog, createdCommentId)
      case None => CreateBlogCommentResult.BlogNotFound
    }

  def createBlogCommentWithNotificationRecipients(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    parentCommentId: Option[BlogCommentId],
    request: CreateBlogCommentRequest
  ): IO[CreateBlogCommentWithNotificationRecipientsResult] =
    createBlogComment(connection, actor, blogId, parentCommentId, request).flatMap {
      case created @ CreateBlogCommentResult.Created(_, createdCommentId) =>
        BlogCommentTable.findCommentNotificationContext(connection, blogId, createdCommentId).flatMap {
          case Some(context) =>
            NotificationCommands
              .createBlogReplyNotifications(connection, actor, context)
              .map(recipients => CreateBlogCommentWithNotificationRecipientsResult(created, recipients))
          case None =>
            IO.pure(CreateBlogCommentWithNotificationRecipientsResult(created, Nil))
        }
      case result =>
        IO.pure(CreateBlogCommentWithNotificationRecipientsResult(result, Nil))
    }

  def voteBlogComment(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    commentId: BlogCommentId,
    request: VoteBlogCommentRequest
  ): IO[VoteBlogCommentResult] =
    BlogCommentVoteTable.voteComment(connection, blogId, commentId, actor.username, request.vote).map {
      case Some(blog) => VoteBlogCommentResult.Voted(blog)
      case None => VoteBlogCommentResult.NotFound
    }

  def updateBlogComment(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    commentId: BlogCommentId,
    request: UpdateBlogCommentRequest
  ): IO[UpdateBlogCommentResult] =
    BlogCommentTable.updateComment(connection, blogId, commentId, actor.username, request.content).map {
      case Some(blog) => UpdateBlogCommentResult.Updated(blog)
      case None => UpdateBlogCommentResult.NotFound
    }

  def deleteBlogComment(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    commentId: BlogCommentId
  ): IO[DeleteBlogCommentResult] =
    BlogCommentTable.deleteComment(connection, blogId, commentId, actor.username).map {
      case Some(blog) => DeleteBlogCommentResult.Deleted(blog)
      case None => DeleteBlogCommentResult.NotFound
    }

  def linkBlogToProblem(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[LinkBlogToProblemResult] =
    ProblemCommands.resolveBlogProblemLinkTarget(connection, actor, problemSlug).flatMap {
      case ProblemCommands.ResolveBlogProblemLinkTargetResult.Forbidden =>
        IO.pure(LinkBlogToProblemResult.Forbidden)
      case ProblemCommands.ResolveBlogProblemLinkTargetResult.ProblemNotFound =>
        IO.pure(LinkBlogToProblemResult.NotFound)
      case ProblemCommands.ResolveBlogProblemLinkTargetResult.Allowed =>
        BlogProblemLinkMutationTable.linkProblem(connection, problemSlug, blogId, actor.username).map {
          case true => LinkBlogToProblemResult.Linked
          case false => LinkBlogToProblemResult.NotFound
        }
    }

  def acceptBlogProblemSubmission(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[AcceptBlogProblemSubmissionResult] =
    if !ProblemCommands.canManageProblemCatalog(actor) then IO.pure(AcceptBlogProblemSubmissionResult.Forbidden)
    else
      BlogProblemLinkMutationTable.acceptProblem(connection, problemSlug, blogId, actor.username).map {
        case true => AcceptBlogProblemSubmissionResult.Accepted
        case false => AcceptBlogProblemSubmissionResult.NotFound
      }

  def unlinkBlogFromProblem(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[UnlinkBlogFromProblemResult] =
    if !ProblemCommands.canManageProblemCatalog(actor) then IO.pure(UnlinkBlogFromProblemResult.Forbidden)
    else
      BlogProblemLinkMutationTable.unlinkProblem(connection, problemSlug, blogId).map {
        case true => UnlinkBlogFromProblemResult.Unlinked
        case false => UnlinkBlogFromProblemResult.NotFound
      }
