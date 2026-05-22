package domains.problemset.application.output

import domains.problemset.model.*

import domains.shared.model.PageResponse

type ProblemSetListResponse = PageResponse[ProblemSetSummary]
