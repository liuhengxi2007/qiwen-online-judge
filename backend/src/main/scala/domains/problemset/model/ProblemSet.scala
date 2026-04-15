package domains.problemset.model

import domains.auth.model.Username
import domains.shared.access.ResourceAccessPolicy

import java.time.Instant

final case class ProblemSet(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  creatorUsername: Username,
  createdAt: Instant,
  updatedAt: Instant
)
