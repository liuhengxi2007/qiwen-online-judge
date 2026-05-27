import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetId } from '@/objects/problemset/ProblemSetId'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'

export type ProblemSetSummary = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  creator: UserIdentity
}
