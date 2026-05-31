package domains.problemset.objects



import domains.user.objects.UserIdentity
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

final case class ProblemSet(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  author: Option[UserIdentity],
  createdAt: Instant,
  updatedAt: Instant
)
