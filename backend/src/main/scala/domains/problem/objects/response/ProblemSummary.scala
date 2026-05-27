package domains.problem.objects.response

import domains.problem.objects.*

import domains.user.objects.UserIdentity
import shared.objects.access.ResourceAccessPolicy

import java.time.Instant

final case class ProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  data: ProblemData,
  ready: Boolean,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  othersSubmissionAccess: OthersSubmissionAccess,
  creator: UserIdentity,
  createdAt: Instant,
  updatedAt: Instant
)
