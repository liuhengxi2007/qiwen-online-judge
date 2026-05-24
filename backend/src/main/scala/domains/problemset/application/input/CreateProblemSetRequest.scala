package domains.problemset.application.input

import domains.problemset.model.*

import shared.model.access.ResourceAccessPolicy

final case class CreateProblemSetRequest(
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)
