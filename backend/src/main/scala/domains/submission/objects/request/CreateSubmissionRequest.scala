package domains.submission.objects.request

import domains.submission.objects.*

import domains.problem.objects.ProblemSlug

final case class CreateSubmissionRequest(
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode
)
