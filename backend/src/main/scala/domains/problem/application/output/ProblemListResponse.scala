package domains.problem.application.output

import domains.problem.model.*

import domains.shared.model.PageResponse

type ProblemListResponse = PageResponse[ProblemSummary]
