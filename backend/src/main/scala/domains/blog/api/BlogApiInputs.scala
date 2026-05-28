package domains.blog.api

import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.{CreateBlogCommentRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.problem.objects.ProblemSlug
import domains.user.objects.Username
import shared.objects.PageRequest

private[api] final case class ListBlogsInput(authorUsername: Option[Username], pageRequest: PageRequest)

private[api] final case class ProblemBlogsInput(problemSlug: ProblemSlug, pageRequest: PageRequest)

private[api] final case class BlogProblemLinkInput(problemSlug: ProblemSlug, blogId: BlogId)

private[api] final case class UpdateBlogInput(blogId: BlogId, request: UpdateBlogRequest)

private[api] final case class VoteBlogInput(blogId: BlogId, request: VoteBlogRequest)

private[api] final case class CreateBlogCommentInput(
  blogId: BlogId,
  parentCommentId: Option[BlogCommentId],
  request: CreateBlogCommentRequest
)

private[api] final case class VoteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: VoteBlogCommentRequest)

private[api] final case class UpdateBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest)

private[api] final case class DeleteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId)
