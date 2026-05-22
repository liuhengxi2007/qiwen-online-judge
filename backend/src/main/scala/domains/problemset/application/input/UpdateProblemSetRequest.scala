package domains.problemset.application.input

import domains.problemset.model.*

import shared.access.ResourceAccessPolicy

final case class UpdateProblemSetRequest(
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)
