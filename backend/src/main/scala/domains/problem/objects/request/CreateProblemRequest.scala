package domains.problem.objects.request

import domains.problem.objects.*

import shared.objects.access.ResourceAccessPolicy

final case class CreateProblemRequest(
  slug: ProblemSlug,
  title: ProblemTitle,
  statement: ProblemStatementText,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  accessPolicy: ResourceAccessPolicy,
  othersSubmissionAccess: OthersSubmissionAccess
)
