package domains.problem.application.input

import domains.problem.model.*

import shared.access.ResourceAccessPolicy

final case class CreateProblemRequest(
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  othersSubmissionAccess: OthersSubmissionAccess
)
