package domains.problemset.model.request


import domains.problem.model.ProblemSlug

final case class AddProblemToProblemSetRequest(
  problemSlug: ProblemSlug
)
