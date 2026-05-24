package domains.problemset.application.output

import domains.problemset.model.*

import domains.user.model.UserIdentity
import shared.model.access.ResourceAccessPolicy

import java.time.Instant

final case class ProblemSetDetail(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  problems: List[ProblemSetProblemSummary],
  accessPolicy: ResourceAccessPolicy,
  creator: UserIdentity,
  createdAt: Instant,
  updatedAt: Instant
)
