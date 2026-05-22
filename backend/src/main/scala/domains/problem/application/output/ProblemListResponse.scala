package domains.problem.application.output

import domains.problem.model.*

import shared.model.PageResponse

type ProblemListResponse = PageResponse[ProblemSummary]
