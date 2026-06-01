import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import { fromOtherUserSubmissionAccessContract } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemData } from '@/objects/problem/ProblemData'
import { fromProblemDataContract } from '@/objects/problem/ProblemData'
import type { ProblemId } from '@/objects/problem/ProblemId'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { ProblemStatementText } from '@/objects/problem/ProblemStatementText'
import { fromProblemStatementTextContract } from '@/objects/problem/ProblemStatementText'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { fromProblemTitleContract } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import { fromResourceAccessPolicyContract } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'
import { fromAuditFieldsContract } from '@/objects/shared/AuditFields'
import { readBoolean, readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemDetail = AuditFields & {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  data: ProblemData
  ready: boolean
  accessPolicy: ResourceAccessPolicy
  otherUserSubmissionAccess: OtherUserSubmissionAccess
  author: UserIdentity | null
  canManage: boolean
}

export function fromProblemDetailContract(value: unknown, label = 'problem detail'): ProblemDetail {
  const problem = readRecord(value, label)
  return {
    ...fromAuditFieldsContract(value, label),
    id: fromProblemIdContract(readString(problem.id, `${label} id`), `${label} id`),
    slug: fromProblemSlugContract(readString(problem.slug, `${label} slug`), `${label} slug`),
    title: fromProblemTitleContract(readString(problem.title, `${label} title`), `${label} title`),
    statement: fromProblemStatementTextContract(readString(problem.statement, `${label} statement`), `${label} statement`),
    data: fromProblemDataContract(readNullable(problem.data, `${label} data`, readString), `${label} data`),
    ready: readBoolean(problem.ready, `${label} ready`),
    accessPolicy: fromResourceAccessPolicyContract(problem.accessPolicy),
    otherUserSubmissionAccess: fromOtherUserSubmissionAccessContract(problem.otherUserSubmissionAccess),
    author: readNullable(problem.author, `${label} author`, fromUserIdentityContract),
    canManage: readBoolean(problem.canManage, `${label} can manage`),
  }
}
