package domains.problem.application

import domains.problem.model.{CreateProblemRequest, ProblemSlug, ProblemStatementText, ProblemTitle, UpdateProblemRequest}

object ProblemValidation:

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
    ProblemSlug.parse(slug.value)

  private def validateTitle(title: ProblemTitle): Either[String, ProblemTitle] =
    ProblemTitle.parse(title.value)

  private def validateStatement(statement: ProblemStatementText): Either[String, ProblemStatementText] =
    ProblemStatementText.parse(statement.value)
