package domains.submission.application



import domains.submission.application.input.CreateSubmissionRequest

object SubmissionValidation:

  def validateCreate(request: CreateSubmissionRequest): Either[String, CreateSubmissionRequest] =
    for
      _ <- domains.problem.model.ProblemSlug.parse(request.problemSlug.value)
      _ <- domains.submission.model.SubmissionSourceCode.parse(request.sourceCode.value)
    yield request
