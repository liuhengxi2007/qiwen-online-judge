package domains.problem.model.request


import shared.model.PageRequest

final case class ProblemListRequest(
  query: Option[ProblemSearchQuery],
  pageRequest: PageRequest
)
