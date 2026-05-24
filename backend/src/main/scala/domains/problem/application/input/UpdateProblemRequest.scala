package domains.problem.application.input

import domains.problem.model.*

import shared.model.access.ResourceAccessPolicy

final case class UpdateProblemRequest(
  title: ProblemTitle,
  statement: ProblemStatementText,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  othersSubmissionAccess: OthersSubmissionAccess
)
