package domains.submission.application



import domains.submission.objects.request.CreateSubmissionRequest

object SubmissionValidation:

  def validateCreate(request: CreateSubmissionRequest): Either[String, CreateSubmissionRequest] =
    for
      _ <- domains.problem.objects.ProblemSlug.parse(request.problemSlug.value)
      _ <- domains.submission.objects.SubmissionSourceCode.parse(request.sourceCode.value)
    yield request
