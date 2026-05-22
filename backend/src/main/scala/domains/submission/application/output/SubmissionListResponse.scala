package domains.submission.application.output

import domains.submission.model.*

import domains.shared.model.PageResponse

type SubmissionListResponse = PageResponse[SubmissionSummary]
