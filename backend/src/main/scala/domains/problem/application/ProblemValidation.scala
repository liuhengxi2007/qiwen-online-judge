package domains.problem.application



import domains.problem.application.input.{CreateProblemRequest, UpdateProblemRequest}
import domains.problem.model.{ProblemDataFilename, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle}

object ProblemValidation:

  def validateCreate(request: CreateProblemRequest): Either[String, CreateProblemRequest] =
    for
      slug <- validateSlug(request.slug)
      title <- validateTitle(request.title)
      statement <- validateStatement(request.statement)
      timeLimitMs <- validateTimeLimitMs(request.timeLimitMs)
      spaceLimitMb <- validateSpaceLimitMb(request.spaceLimitMb)
    yield request.copy(
      slug = slug,
      title = title,
      statement = statement,
      timeLimitMs = timeLimitMs,
      spaceLimitMb = spaceLimitMb
    )

  def validateUpdate(request: UpdateProblemRequest): Either[String, UpdateProblemRequest] =
    for
      title <- validateTitle(request.title)
      statement <- validateStatement(request.statement)
      timeLimitMs <- validateTimeLimitMs(request.timeLimitMs)
      spaceLimitMb <- validateSpaceLimitMb(request.spaceLimitMb)
    yield request.copy(
      title = title,
      statement = statement,
      timeLimitMs = timeLimitMs,
      spaceLimitMb = spaceLimitMb
    )

  private def validateSlug(slug: ProblemSlug): Either[String, ProblemSlug] =
    ProblemSlug.parse(slug.value)

  private def validateTitle(title: ProblemTitle): Either[String, ProblemTitle] =
    ProblemTitle.parse(title.value)

  private def validateStatement(statement: ProblemStatementText): Either[String, ProblemStatementText] =
    ProblemStatementText.parse(statement.value)

  private def validateTimeLimitMs(timeLimitMs: ProblemTimeLimitMs): Either[String, ProblemTimeLimitMs] =
    ProblemTimeLimitMs.parse(timeLimitMs.value)

  private def validateSpaceLimitMb(spaceLimitMb: ProblemSpaceLimitMb): Either[String, ProblemSpaceLimitMb] =
    ProblemSpaceLimitMb.parse(spaceLimitMb.value)

  private def validateFilename(filename: ProblemDataFilename): Either[String, ProblemDataFilename] =
    ProblemDataFilename.parse(filename.value)
