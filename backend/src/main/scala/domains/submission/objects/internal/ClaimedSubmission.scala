package domains.submission.objects.internal

import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionSourceCode}

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode
)
