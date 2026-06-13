import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetId } from '@/objects/problemset/ProblemSetId'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'

/** 题集摘要响应；用于列表页，不包含题集内题目明细。 */
export type ProblemSetSummary = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  author: UserIdentity | null
}
