package domains.submission.objects.internal

import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.SubmissionId

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  programManifest: SubmissionProgramManifest
)
