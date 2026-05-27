package domains.problem.objects.request


import shared.objects.PageRequest

final case class ProblemListRequest(
  query: Option[ProblemSearchQuery],
  pageRequest: PageRequest
)
