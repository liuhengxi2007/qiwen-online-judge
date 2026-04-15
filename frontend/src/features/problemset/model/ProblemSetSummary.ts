import type { Username } from '@/features/auth/model/AuthValues'
import type { ProblemSetDescription } from '@/features/problemset/model/ProblemSetDescription'
import type { ProblemSetId } from '@/features/problemset/model/ProblemSetId'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import type { ProblemSetTitle } from '@/features/problemset/model/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'
import type { AuditFields } from '@/shared/model/AuditFields'

export type ProblemSetSummary = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  creatorUsername: Username
}
