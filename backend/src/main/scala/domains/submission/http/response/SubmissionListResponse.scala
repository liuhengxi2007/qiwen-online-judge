package domains.submission.http.response

import domains.submission.model.*

import domains.shared.model.PageResponse

type SubmissionListResponse = PageResponse[SubmissionSummary]
