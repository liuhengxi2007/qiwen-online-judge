package domains.submission.application.input

import domains.submission.model.*

import shared.model.PageRequest

final case class SubmissionListRequest(
  userQuery: Option[SubmissionUserQuery],
  problemQuery: Option[SubmissionProblemQuery],
  verdict: SubmissionVerdictFilter,
  sort: SubmissionSort,
  direction: SubmissionSortDirection,
  pageRequest: PageRequest
)
