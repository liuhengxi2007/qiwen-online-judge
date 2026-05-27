package domains.problemset.http.mapper

import domains.problem.objects.ProblemSlug
import domains.problemset.objects.ProblemSetSlug
import domains.problemset.objects.request.{AddProblemToProblemSetRequest, UpdateProblemSetRequest}
import shared.http.utils.PageRequestQuerySupport
import shared.objects.PageRequest

object ProblemSetHttpRequestMappers:

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
