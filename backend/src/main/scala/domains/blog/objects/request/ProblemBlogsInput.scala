package domains.blog.objects.request

import domains.problem.objects.ProblemSlug
import shared.objects.PageRequest

/** 题目博客列表查询输入；包含题目 slug 和分页参数。 */
final case class ProblemBlogsInput(
  problemSlug: ProblemSlug,
  pageRequest: PageRequest
)
