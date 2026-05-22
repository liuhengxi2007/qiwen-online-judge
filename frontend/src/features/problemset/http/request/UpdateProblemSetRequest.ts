import type { ProblemSetDescription } from '@/features/problemset/model/ProblemSetDescription'
import type { ProblemSetTitle } from '@/features/problemset/model/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'

export type UpdateProblemSetRequest = {
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
}
