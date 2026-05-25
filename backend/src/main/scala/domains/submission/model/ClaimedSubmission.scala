package domains.submission.model

import domains.problem.model.{ProblemId, ProblemSlug, ProblemSpaceLimitMb, ProblemTimeLimitMs}

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb
)
