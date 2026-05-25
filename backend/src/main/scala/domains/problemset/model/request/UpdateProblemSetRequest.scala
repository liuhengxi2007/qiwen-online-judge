package domains.problemset.model.request

import domains.problemset.model.*

import shared.model.access.ResourceAccessPolicy

final case class UpdateProblemSetRequest(
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)
