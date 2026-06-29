import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetId } from '@/objects/problemset/ProblemSetId'
import type { ProblemSetProblemSummary } from '@/objects/problemset/ProblemSetProblemSummary'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'

/** 题集详情响应；包含题目顺序、访问策略和作者信息。 */
export type ProblemSetDetail = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceVisibilityPolicy
  author: UserIdentity | null
}
