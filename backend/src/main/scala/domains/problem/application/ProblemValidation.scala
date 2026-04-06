package domains.problem.application

import domains.problem.model.{CreateProblemRequest, ProblemDataFilename, ProblemSlug, ProblemSpaceLimitMb, ProblemStatementText, ProblemTimeLimitMs, ProblemTitle, UpdateProblemDataRequest, UpdateProblemRequest}

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

  def validateDataUpdate(request: UpdateProblemDataRequest): Either[String, UpdateProblemDataRequest] =
    for
      filename <- validateFilename(request.filename)
      _ <- validateContentBase64(request.contentBase64)
    yield request.copy(filename = filename)

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

  private def validateContentBase64(contentBase64: String): Either[String, String] =
    if contentBase64.trim.isEmpty then Left("Problem data file content is required.")
    else Right(contentBase64)
