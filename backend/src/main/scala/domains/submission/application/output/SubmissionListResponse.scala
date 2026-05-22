package domains.submission.application.output

import domains.submission.model.*

import shared.model.PageResponse

type SubmissionListResponse = PageResponse[SubmissionSummary]
