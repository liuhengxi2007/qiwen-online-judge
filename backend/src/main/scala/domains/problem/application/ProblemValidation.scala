package domains.problem.application

import domains.problem.model.{CreateProblemRequest, ProblemSlug, ProblemStatementText, ProblemTitle, UpdateProblemRequest}

object ProblemValidation:

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def validateCreate(request: CreateProblemRequest): Either[String, CreateProblemRequest] =
    for
      slug <- validateSlug(request.slug)
      title <- validateTitle(request.title)
      statement <- validateStatement(request.statement)
    yield request.copy(
      slug = slug,
      title = title,
      statement = statement
    )

  def validateUpdate(request: UpdateProblemRequest): Either[String, UpdateProblemRequest] =
    for
      title <- validateTitle(request.title)
      statement <- validateStatement(request.statement)
    yield request.copy(
      title = title,
      statement = statement
    )

  private def validateSlug(slug: ProblemSlug): Either[String, ProblemSlug] =
    val normalized = slug.value.trim
    if normalized.isEmpty then Left("Problem slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSlug(normalized))

  private def validateTitle(title: ProblemTitle): Either[String, ProblemTitle] =
    val normalized = title.value.trim
    if normalized.isEmpty then Left("Problem title is required.")
    else if normalized.length > 120 then Left("Problem title must be at most 120 characters.")
    else Right(ProblemTitle(normalized))

  private def validateStatement(statement: ProblemStatementText): Either[String, ProblemStatementText] =
    val normalized = statement.value.trim
    if normalized.isEmpty then Left("Problem statement is required.")
    else if normalized.length > 20000 then Left("Problem statement must be at most 20000 characters.")
    else Right(ProblemStatementText(normalized))
