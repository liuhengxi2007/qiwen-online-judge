package domains.problemset.model

import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}

final case class ProblemSetProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  position: Int
)
