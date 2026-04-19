package domains.blog.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.blog.application.BlogCommandResults.*
import domains.blog.model.{BlogCommentId, BlogId, CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.blog.table.BlogTable
import domains.problem.application.ProblemPolicy
import domains.problem.model.ProblemSlug
import domains.problem.table.ProblemTable

import java.sql.Connection

object BlogMutationCommands:

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
        BlogTable
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
    BlogTable.vote(connection, blogId, actor.username, request.vote).map {
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
        BlogTable
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
    BlogTable.delete(connection, blogId, actor.username).map {
      case true => DeleteBlogResult.Deleted
      case false => DeleteBlogResult.NotFound
    }

  def submitBlogToProblem(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[SubmitBlogToProblemResult] =
    BlogTable.submitProblem(connection, problemSlug, blogId, actor.username).map {
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
    BlogTable.insertComment(connection, blogId, parentCommentId, actor.username, request.content).map {
      case Some(blog) => CreateBlogCommentResult.Created(blog)
      case None => CreateBlogCommentResult.BlogNotFound
    }

  def voteBlogComment(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    commentId: BlogCommentId,
    request: VoteBlogCommentRequest
  ): IO[VoteBlogCommentResult] =
    BlogTable.voteComment(connection, blogId, commentId, actor.username, request.vote).map {
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
    BlogTable.updateComment(connection, blogId, commentId, actor.username, request.content).map {
      case Some(blog) => UpdateBlogCommentResult.Updated(blog)
      case None => UpdateBlogCommentResult.NotFound
    }

  def deleteBlogComment(
    connection: Connection,
    actor: AuthUser,
    blogId: BlogId,
    commentId: BlogCommentId
  ): IO[DeleteBlogCommentResult] =
    BlogTable.deleteComment(connection, blogId, commentId, actor.username).map {
      case Some(blog) => DeleteBlogCommentResult.Deleted(blog)
      case None => DeleteBlogCommentResult.NotFound
    }

  def linkBlogToProblem(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[LinkBlogToProblemResult] =
    if !ProblemPolicy.canEdit(actor) then IO.pure(LinkBlogToProblemResult.Forbidden)
    else
      ProblemTable.findBySlug(connection, problemSlug).flatMap {
        case None => IO.pure(LinkBlogToProblemResult.NotFound)
        case Some(_) =>
          BlogTable.linkProblem(connection, problemSlug, blogId, actor.username).map {
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
    if !ProblemPolicy.canEdit(actor) then IO.pure(AcceptBlogProblemSubmissionResult.Forbidden)
    else
      BlogTable.acceptProblem(connection, problemSlug, blogId, actor.username).map {
        case true => AcceptBlogProblemSubmissionResult.Accepted
        case false => AcceptBlogProblemSubmissionResult.NotFound
      }

  def unlinkBlogFromProblem(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    blogId: BlogId
  ): IO[UnlinkBlogFromProblemResult] =
    if !ProblemPolicy.canEdit(actor) then IO.pure(UnlinkBlogFromProblemResult.Forbidden)
    else
      BlogTable.unlinkProblem(connection, problemSlug, blogId).map {
        case true => UnlinkBlogFromProblemResult.Unlinked
        case false => UnlinkBlogFromProblemResult.NotFound
      }
