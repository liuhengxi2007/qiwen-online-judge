package domains.blog.api

import domains.problem.objects.ProblemSlug
import shared.objects.PageRequest

private[api] final case class ProblemBlogsInput(
  problemSlug: ProblemSlug,
  pageRequest: PageRequest
)
