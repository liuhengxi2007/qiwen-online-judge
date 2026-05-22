package domains.problemset.http.response

import domains.problemset.model.*

import domains.shared.model.PageResponse

type ProblemSetListResponse = PageResponse[ProblemSetSummary]
