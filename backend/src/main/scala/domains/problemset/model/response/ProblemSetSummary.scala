package domains.problemset.model.response

import domains.problemset.model.*

import domains.user.model.UserIdentity
import shared.model.access.ResourceAccessPolicy

import java.time.Instant

final case class ProblemSetSummary(
  id: ProblemSetId,
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy,
  creator: UserIdentity,
  createdAt: Instant,
  updatedAt: Instant
)
