package domains.problemset.objects.request

import domains.problemset.objects.*

import shared.objects.access.ResourceAccessPolicy

final case class CreateProblemSetRequest(
  slug: ProblemSetSlug,
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)
