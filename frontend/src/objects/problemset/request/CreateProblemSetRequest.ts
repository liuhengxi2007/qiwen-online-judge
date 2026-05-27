import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/AccessPolicy'

export type CreateProblemSetRequest = {
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
}
