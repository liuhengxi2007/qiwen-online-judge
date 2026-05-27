package domains.user.objects



import domains.problem.objects.{ProblemSlug, ProblemTitle}

import java.time.Instant

final case class UserAcceptedProblem(
  slug: ProblemSlug,
  title: ProblemTitle,
  acceptedAt: Instant
)
