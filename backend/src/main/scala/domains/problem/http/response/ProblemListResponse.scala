package domains.problem.http.response

import domains.problem.model.*

import domains.shared.model.PageResponse

type ProblemListResponse = PageResponse[ProblemSummary]
