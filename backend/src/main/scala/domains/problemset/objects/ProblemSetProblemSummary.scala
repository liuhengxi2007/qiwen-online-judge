package domains.problemset.objects

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}

final case class ProblemSetProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  position: Int
)
