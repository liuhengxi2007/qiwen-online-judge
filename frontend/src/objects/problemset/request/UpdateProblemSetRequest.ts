import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

export type UpdateProblemSetRequest = {
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
}
