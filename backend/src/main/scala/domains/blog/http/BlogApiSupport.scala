package domains.blog.http

import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.{CreateBlogCommentRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.problem.objects.ProblemSlug
import domains.user.objects.Username
import shared.http.ApiMessage
import shared.objects.PageRequest
import shared.objects.response.SuccessResponse

object BlogApiSupport:

  final case class ListBlogsInput(authorUsername: Option[Username], pageRequest: PageRequest)

  final case class ProblemBlogsInput(problemSlug: ProblemSlug, pageRequest: PageRequest)

  final case class BlogProblemLinkInput(problemSlug: ProblemSlug, blogId: BlogId)

  final case class UpdateBlogInput(blogId: BlogId, request: UpdateBlogRequest)

  final case class VoteBlogInput(blogId: BlogId, request: VoteBlogRequest)

  final case class CreateBlogCommentInput(
    blogId: BlogId,
    parentCommentId: Option[BlogCommentId],
    request: CreateBlogCommentRequest
  )

  final case class VoteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: VoteBlogCommentRequest)

  final case class UpdateBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest)

  final case class DeleteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId)

  def success(message: ApiMessage): SuccessResponse =
    SuccessResponse(code = Some(message.code), message = None, params = message.params)
