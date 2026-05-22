package domains.problemset.application.input

import domains.problemset.model.*

import domains.problem.model.ProblemSlug

final case class AddProblemToProblemSetRequest(
  problemSlug: ProblemSlug
)
