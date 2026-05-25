package domains.submission.http.mapper

import domains.submission.model.SubmissionId
import domains.submission.model.request.{
  SubmissionListRequest,
  SubmissionProblemQuery,
  SubmissionSort,
  SubmissionSortDirection,
  SubmissionUserQuery,
  SubmissionVerdictFilter
}
import shared.http.utils.PageRequestQuerySupport

object SubmissionHttpRequestMappers:

  def submissionId(rawSubmissionId: String): Either[String, SubmissionId] =
    SubmissionId.parse(rawSubmissionId)

  def listSubmissionsRequest(queryParams: Map[String, String]): SubmissionListRequest =
    val sort = queryParams
      .get("sort")
      .flatMap(rawSort => SubmissionSort.parse(rawSort).toOption)
      .getOrElse(SubmissionSort.Submitted)
    val direction = queryParams
      .get("direction")
      .flatMap(rawDirection => SubmissionSortDirection.parse(rawDirection).toOption)
      .getOrElse(SubmissionSort.defaultDirection(sort))
    val verdict = queryParams
      .get("verdict")
      .flatMap(rawVerdict => SubmissionVerdictFilter.parse(rawVerdict).toOption)
      .getOrElse(SubmissionVerdictFilter.All)

    SubmissionListRequest(
      userQuery = queryParams
        .get("username")
        .flatMap(rawQuery => SubmissionUserQuery.parse(rawQuery).toOption),
      problemQuery = queryParams
        .get("problem")
        .flatMap(rawQuery => SubmissionProblemQuery.parse(rawQuery).toOption),
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
    )
