package domains.problem.application.input

import domains.problem.model.*

import shared.model.PageRequest

final case class ProblemListRequest(
  query: Option[ProblemSearchQuery],
  pageRequest: PageRequest
)
