package domains.problem.http.mapper

import domains.problem.http.ProblemHttpPlans.SetProblemReadyRequest
import domains.problem.model.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.model.request.{CreateProblemRequest, DeleteProblemDataPathRequest, ProblemListRequest, ProblemSearchQuery, UpdateProblemRequest}
import shared.http.utils.PageRequestQuerySupport

object ProblemHttpRequestMappers:

  def createProblemRequest(body: CreateProblemRequest): CreateProblemRequest =
    body

  def problemSlug(rawProblemSlug: String): Either[String, ProblemSlug] =
    ProblemSlug.parse(rawProblemSlug)

  def listProblemsRequest(queryParams: Map[String, String]): ProblemListRequest =
    ProblemListRequest(
      query = queryParams.get("q").flatMap(rawQuery => ProblemSearchQuery.parse(rawQuery).toOption),
      pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
    )

  def problemSearchQuery(queryParams: Map[String, String]): Either[String, ProblemSearchQuery] =
    ProblemSearchQuery.parse(queryParams.get("q").getOrElse(""))

  def problemSlugAndFilename(rawProblemSlug: String, rawFilename: String): Either[String, (ProblemSlug, ProblemDataFilename)] =
    (ProblemSlug.parse(rawProblemSlug), ProblemDataFilename.parse(rawFilename)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(problemSlug), Right(filename)) => Right((problemSlug, filename))

  def problemSlugAndPath(rawProblemSlug: String, rawPath: String): Either[String, (ProblemSlug, ProblemDataPath)] =
    (ProblemSlug.parse(rawProblemSlug), ProblemDataPath.parse(rawPath)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(problemSlug), Right(path)) => Right((problemSlug, path))

  def deleteProblemDataPathInput(
    rawProblemSlug: String,
    body: DeleteProblemDataPathRequest
  ): Either[String, (ProblemSlug, DeleteProblemDataPathRequest)] =
    ProblemSlug.parse(rawProblemSlug).map(problemSlug => (problemSlug, body))

  def setProblemReadyInput(
    rawProblemSlug: String,
    body: SetProblemReadyRequest
  ): Either[String, (ProblemSlug, SetProblemReadyRequest)] =
    ProblemSlug.parse(rawProblemSlug).map(problemSlug => (problemSlug, body))

  def updateProblemInput(
    rawProblemSlug: String,
    body: UpdateProblemRequest
  ): Either[String, (ProblemSlug, UpdateProblemRequest)] =
    ProblemSlug.parse(rawProblemSlug).map(problemSlug => (problemSlug, body))
