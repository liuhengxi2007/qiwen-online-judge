package domains.problemset.application

import domains.problem.model.ProblemSlug
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetDescription, ProblemSetSlug, ProblemSetTitle, UpdateProblemSetRequest}

object ProblemSetValidation:

  def validateCreate(request: CreateProblemSetRequest): Either[String, CreateProblemSetRequest] =
    for
      slug <- validateSlug(request.slug)
      title <- validateTitle(request.title)
      description <- validateDescription(request.description)
    yield request.copy(
      slug = slug,
      title = title,
      description = description
    )

  def validateAddProblem(request: AddProblemToProblemSetRequest): Either[String, AddProblemToProblemSetRequest] =
    validateProblemSlug(request.problemSlug).map(validSlug => request.copy(problemSlug = validSlug))

  def validateUpdate(request: UpdateProblemSetRequest): Either[String, UpdateProblemSetRequest] =
    for
      title <- validateTitle(request.title)
      description <- validateDescription(request.description)
    yield request.copy(
      title = title,
      description = description
    )

  private def validateSlug(slug: ProblemSetSlug): Either[String, ProblemSetSlug] =
    ProblemSetSlug.parse(slug.value)

  private def validateTitle(title: ProblemSetTitle): Either[String, ProblemSetTitle] =
    ProblemSetTitle.parse(title.value)

  private def validateDescription(description: ProblemSetDescription): Either[String, ProblemSetDescription] =
    ProblemSetDescription.parse(description.value)

  private def validateProblemSlug(problemSlug: ProblemSlug): Either[String, ProblemSlug] =
    ProblemSlug.parse(problemSlug.value)
