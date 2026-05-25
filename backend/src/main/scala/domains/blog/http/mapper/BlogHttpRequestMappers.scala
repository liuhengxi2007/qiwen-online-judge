package domains.blog.http.mapper

import domains.blog.http.BlogHttpPlans
import domains.blog.model.{BlogCommentId, BlogId}
import domains.blog.model.request.{CreateBlogCommentRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.problem.model.ProblemSlug
import domains.user.model.Username
import shared.http.utils.PageRequestQuerySupport

object BlogHttpRequestMappers:

  def blogId(rawBlogId: String): Either[String, BlogId] =
    BlogId.parse(rawBlogId)

  def blogCommentId(rawCommentId: String): Either[String, BlogCommentId] =
    BlogCommentId.parse(rawCommentId)

  def blogProblemLinkInput(rawProblemSlug: String, rawBlogId: String): Either[String, BlogHttpPlans.BlogProblemLinkInput] =
    (ProblemSlug.parse(rawProblemSlug), BlogId.parse(rawBlogId)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(problemSlug), Right(blogId)) => Right(BlogHttpPlans.BlogProblemLinkInput(problemSlug, blogId))

  def createBlogCommentInput(
    rawBlogId: String,
    commentRequest: CreateBlogCommentRequest
  ): Either[String, BlogHttpPlans.CreateBlogCommentInput] =
    BlogId.parse(rawBlogId).map(blogId => BlogHttpPlans.CreateBlogCommentInput(blogId, None, commentRequest))

  def createBlogCommentReplyInput(
    rawBlogId: String,
    rawCommentId: String,
    commentRequest: CreateBlogCommentRequest
  ): Either[String, BlogHttpPlans.CreateBlogCommentInput] =
    (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(blogId), Right(commentId)) => Right(BlogHttpPlans.CreateBlogCommentInput(blogId, Some(commentId), commentRequest))

  def deleteBlogCommentInput(rawBlogId: String, rawCommentId: String): Either[String, BlogHttpPlans.DeleteBlogCommentInput] =
    (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(blogId), Right(commentId)) => Right(BlogHttpPlans.DeleteBlogCommentInput(blogId, commentId))

  def listBlogsInput(queryParams: Map[String, String]): BlogHttpPlans.ListBlogsInput =
    BlogHttpPlans.ListBlogsInput(
      queryParams.get("username").map(Username.canonical),
      PageRequestQuerySupport.parsePageRequest(queryParams)
    )

  def problemBlogsInput(rawProblemSlug: String, queryParams: Map[String, String]): Either[String, BlogHttpPlans.ProblemBlogsInput] =
    ProblemSlug.parse(rawProblemSlug).map(problemSlug =>
      BlogHttpPlans.ProblemBlogsInput(problemSlug, PageRequestQuerySupport.parsePageRequest(queryParams))
    )

  def updateBlogInput(rawBlogId: String, updateRequest: UpdateBlogRequest): Either[String, BlogHttpPlans.UpdateBlogInput] =
    BlogId.parse(rawBlogId).map(blogId => BlogHttpPlans.UpdateBlogInput(blogId, updateRequest))

  def updateBlogCommentInput(
    rawBlogId: String,
    rawCommentId: String,
    updateRequest: UpdateBlogCommentRequest
  ): Either[String, BlogHttpPlans.UpdateBlogCommentInput] =
    (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(blogId), Right(commentId)) => Right(BlogHttpPlans.UpdateBlogCommentInput(blogId, commentId, updateRequest))

  def voteBlogInput(rawBlogId: String, voteRequest: VoteBlogRequest): Either[String, BlogHttpPlans.VoteBlogInput] =
    BlogId.parse(rawBlogId).map(blogId => BlogHttpPlans.VoteBlogInput(blogId, voteRequest))

  def voteBlogCommentInput(
    rawBlogId: String,
    rawCommentId: String,
    voteRequest: VoteBlogCommentRequest
  ): Either[String, BlogHttpPlans.VoteBlogCommentInput] =
    (BlogId.parse(rawBlogId), BlogCommentId.parse(rawCommentId)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(blogId), Right(commentId)) => Right(BlogHttpPlans.VoteBlogCommentInput(blogId, commentId, voteRequest))
