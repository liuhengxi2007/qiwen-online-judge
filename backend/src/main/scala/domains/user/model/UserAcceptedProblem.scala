package domains.user.model



import domains.problem.model.{ProblemSlug, ProblemTitle}

import java.time.Instant

final case class UserAcceptedProblem(
  slug: ProblemSlug,
  title: ProblemTitle,
  acceptedAt: Instant
)
