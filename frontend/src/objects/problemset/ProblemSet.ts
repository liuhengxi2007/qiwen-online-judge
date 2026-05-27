import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetId } from '@/objects/problemset/ProblemSetId'
import type { ProblemSetProblemSummary } from '@/objects/problemset/ProblemSetProblemSummary'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/AccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'

export type ProblemSet = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  creator: UserIdentity
}
