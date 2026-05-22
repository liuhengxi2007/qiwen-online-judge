package domains.problemset.application.output

import domains.problemset.model.*

import shared.model.PageResponse

type ProblemSetListResponse = PageResponse[ProblemSetSummary]
