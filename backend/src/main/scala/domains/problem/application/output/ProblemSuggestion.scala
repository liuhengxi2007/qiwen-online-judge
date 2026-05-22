package domains.problem.application.output

import domains.problem.model.*

final case class ProblemSuggestion(
  slug: ProblemSlug,
  title: ProblemTitle
)
