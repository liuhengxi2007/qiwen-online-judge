package domains.problemset.objects.response

import domains.problemset.objects.*

import domains.user.objects.UserIdentity
import shared.objects.access.ResourceAccessPolicy

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
