package domains.blog.objects.request

import domains.blog.objects.BlogId
import domains.problem.objects.ProblemSlug

/** 题目与博客关联操作的输入；由题目 slug 和博客 ID 路径参数组成。 */
final case class BlogProblemLinkInput(
  problemSlug: ProblemSlug,
  blogId: BlogId
)
