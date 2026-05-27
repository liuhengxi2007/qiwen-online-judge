package domains.submission.objects.request


import shared.objects.PageRequest

final case class SubmissionListRequest(
  userQuery: Option[SubmissionUserQuery],
  problemQuery: Option[SubmissionProblemQuery],
  verdict: SubmissionVerdictFilter,
  sort: SubmissionSort,
  direction: SubmissionSortDirection,
  pageRequest: PageRequest
)
