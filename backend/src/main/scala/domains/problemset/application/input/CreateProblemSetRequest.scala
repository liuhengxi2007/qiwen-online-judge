package domains.problemset.application.input

import domains.problemset.model.*

import shared.access.ResourceAccessPolicy

final case class CreateProblemSetRequest(
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)
