import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemData } from '@/objects/problem/ProblemData'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemStatementText } from '@/objects/problem/ProblemStatementText'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'

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
