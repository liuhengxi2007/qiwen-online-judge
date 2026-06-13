package domains.blog.api

import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.{CreateBlogCommentRequest, UpdateBlogCommentRequest, UpdateBlogRequest, VoteBlogCommentRequest, VoteBlogRequest}
import domains.problem.objects.ProblemSlug
import domains.user.objects.Username
import shared.objects.PageRequest

/** 博客列表入口的内部输入，包含可选作者过滤和分页参数。 */
private[api] final case class ListBlogsInput(authorUsername: Option[Username], pageRequest: PageRequest)

/** 问题关联博客列表入口的内部输入，绑定题目 slug 和分页参数。 */
private[api] final case class ProblemBlogsInput(problemSlug: ProblemSlug, pageRequest: PageRequest)

/** 博客与题目关联操作的内部输入，来自路径参数。 */
private[api] final case class BlogProblemLinkInput(problemSlug: ProblemSlug, blogId: BlogId)

/** 更新博客入口的内部输入，绑定路径中的博客 id 和请求体。 */
private[api] final case class UpdateBlogInput(blogId: BlogId, request: UpdateBlogRequest)

/** 博客投票入口的内部输入，绑定博客 id 和投票请求。 */
private[api] final case class VoteBlogInput(blogId: BlogId, request: VoteBlogRequest)

/** 创建博客评论或回复的内部输入，parentCommentId 为 None 时表示顶层评论。 */
private[api] final case class CreateBlogCommentInput(
  blogId: BlogId,
  parentCommentId: Option[BlogCommentId],
  request: CreateBlogCommentRequest
)

/** 评论投票入口的内部输入，绑定博客、评论和投票请求体。 */
private[api] final case class VoteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: VoteBlogCommentRequest)

/** 更新评论入口的内部输入，绑定博客、评论和更新请求体。 */
private[api] final case class UpdateBlogCommentInput(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest)

/** 删除评论入口的内部输入，来自路径中的博客和评论 id。 */
private[api] final case class DeleteBlogCommentInput(blogId: BlogId, commentId: BlogCommentId)
