package domains.submission.model.request

import domains.submission.model.*

import domains.problem.model.ProblemSlug

final case class CreateSubmissionRequest(
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode
)
