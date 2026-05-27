package domains.problemset.objects.request


import domains.problem.objects.ProblemSlug

final case class AddProblemToProblemSetRequest(
  problemSlug: ProblemSlug
)
