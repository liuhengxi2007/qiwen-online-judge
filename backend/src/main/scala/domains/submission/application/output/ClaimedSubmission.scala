package domains.submission.application.output

import domains.problem.model.{ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemTimeLimitMs}
import domains.submission.model.{SubmissionId, SubmissionLanguage, SubmissionSourceCode}

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb
)
