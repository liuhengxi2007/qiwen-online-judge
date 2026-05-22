package domains.problem.application.output

import domains.problem.model.*

import domains.user.model.UserIdentity
import shared.access.ResourceAccessPolicy

import java.time.Instant

final case class ProblemDetail(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  data: ProblemData,
  ready: Boolean,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  othersSubmissionAccess: OthersSubmissionAccess,
  creator: UserIdentity,
  canManage: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)
