package domains.blog.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.blog.application.BlogCommandResults.*
import domains.blog.model.{BlogCommentId, BlogId, BlogType, CreateBlogCommentRequest, CreateBlogRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.blog.table.BlogTable
import domains.problem.model.ProblemId
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
        resolveProblemId(connection, validRequest.blogType, validRequest.problemSlug).flatMap {
          case Left(message) => IO.pure(CreateBlogResult.ValidationFailed(message))
          case Right(problemId) =>
            BlogTable
              .insert(
                connection,
                actor.username,
                validRequest.title,
                validRequest.content,
                validRequest.visibility,
                validRequest.blogType,
                problemId
              )
              .map(blog => CreateBlogResult.Created(blog))
        }

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
        resolveProblemId(connection, validRequest.blogType, validRequest.problemSlug).flatMap {
          case Left(message) => IO.pure(UpdateBlogResult.ValidationFailed(message))
          case Right(problemId) =>
            BlogTable
              .update(
                connection,
                blogId,
                actor.username,
                validRequest.title,
                validRequest.content,
                validRequest.visibility,
                validRequest.blogType,
                problemId
              )
              .map {
                case Some(blog) => UpdateBlogResult.Updated(blog)
                case None => UpdateBlogResult.NotFound
              }
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

  private def resolveProblemId(
    connection: Connection,
    blogType: BlogType,
    problemSlug: Option[domains.problem.model.ProblemSlug]
  ): IO[Either[String, Option[ProblemId]]] =
    blogType match
      case BlogType.General => IO.pure(Right(None))
      case BlogType.Problem =>
        problemSlug match
          case None => IO.pure(Left("Problem blog requires a linked problem."))
          case Some(slug) =>
            ProblemTable.findBySlug(connection, slug).map {
              case Some(problem) => Right(Some(problem.id))
              case None => Left("Linked problem was not found.")
            }
