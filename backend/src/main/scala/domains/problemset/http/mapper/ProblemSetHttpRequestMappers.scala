package domains.problemset.http.mapper

import domains.problem.model.ProblemSlug
import domains.problemset.model.ProblemSetSlug
import domains.problemset.model.request.{AddProblemToProblemSetRequest, CreateProblemSetRequest, UpdateProblemSetRequest}
import shared.http.utils.PageRequestQuerySupport
import shared.model.PageRequest

object ProblemSetHttpRequestMappers:

  def createProblemSetRequest(body: CreateProblemSetRequest): CreateProblemSetRequest =
    body

  def problemSetSlug(rawProblemSetSlug: String): Either[String, ProblemSetSlug] =
    ProblemSetSlug.parse(rawProblemSetSlug)

  def listProblemSetsRequest(queryParams: Map[String, String]): PageRequest =
    PageRequestQuerySupport.parsePageRequest(queryParams)

  def addProblemInput(
    rawProblemSetSlug: String,
    body: AddProblemToProblemSetRequest
  ): Either[String, (ProblemSetSlug, AddProblemToProblemSetRequest)] =
    ProblemSetSlug.parse(rawProblemSetSlug).map(problemSetSlug => (problemSetSlug, body))

  def updateProblemSetInput(
    rawProblemSetSlug: String,
    body: UpdateProblemSetRequest
  ): Either[String, (ProblemSetSlug, UpdateProblemSetRequest)] =
    ProblemSetSlug.parse(rawProblemSetSlug).map(problemSetSlug => (problemSetSlug, body))

  def removeProblemInput(rawProblemSetSlug: String, rawProblemSlug: String): Either[String, (ProblemSetSlug, ProblemSlug)] =
    (ProblemSetSlug.parse(rawProblemSetSlug), ProblemSlug.parse(rawProblemSlug)) match
      case (Left(message), _) => Left(message)
      case (_, Left(message)) => Left(message)
      case (Right(problemSetSlug), Right(problemSlug)) => Right((problemSetSlug, problemSlug))
