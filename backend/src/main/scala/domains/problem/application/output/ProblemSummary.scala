package domains.problem.application.output

import domains.problem.model.*

import domains.user.model.UserIdentity
import shared.model.access.ResourceAccessPolicy

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
