package domains.judge.http.mapper

import domains.problem.model.{ProblemDataPath, ProblemSlug}
import domains.submission.model.SubmissionId

object JudgeHttpRequestMappers:

  def submissionId(rawSubmissionId: String): Either[String, SubmissionId] =
    SubmissionId.parse(rawSubmissionId)

  def problemDataDownloadInput(queryParams: Map[String, String]): Either[String, (ProblemSlug, ProblemDataPath)] =
    val maybeProblemSlug = queryParams.get("problemSlug").flatMap(raw => ProblemSlug.parse(raw).toOption)
    val maybePath = queryParams.get("path").flatMap(raw => ProblemDataPath.parse(raw).toOption)

    (maybeProblemSlug, maybePath) match
      case (Some(problemSlug), Some(path)) => Right((problemSlug, path))
      case _ => Left("Valid problemSlug and path query parameters are required.")
