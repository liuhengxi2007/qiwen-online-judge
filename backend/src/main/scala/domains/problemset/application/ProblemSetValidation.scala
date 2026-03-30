package domains.problemset.application

import domains.problem.model.ProblemSlug
import domains.problemset.model.{AddProblemToProblemSetRequest, CreateProblemSetRequest, ProblemSetDescription, ProblemSetSlug, ProblemSetTitle}

object ProblemSetValidation:

  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

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

  private def validateSlug(slug: ProblemSetSlug): Either[String, ProblemSetSlug] =
    val normalized = slug.value.trim
    if normalized.isEmpty then Left("Problem set slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem set slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem set slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSetSlug(normalized))

  private def validateTitle(title: ProblemSetTitle): Either[String, ProblemSetTitle] =
    val normalized = title.value.trim
    if normalized.isEmpty then Left("Problem set title is required.")
    else if normalized.length > 120 then Left("Problem set title must be at most 120 characters.")
    else Right(ProblemSetTitle(normalized))

  private def validateDescription(description: ProblemSetDescription): Either[String, ProblemSetDescription] =
    val normalized = description.value.trim
    if normalized.length > 2000 then Left("Problem set description must be at most 2000 characters.")
    else Right(ProblemSetDescription(normalized))

  private def validateProblemSlug(problemSlug: ProblemSlug): Either[String, ProblemSlug] =
    val normalized = problemSlug.value.trim
    if normalized.isEmpty then Left("Problem slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSlug(normalized))
