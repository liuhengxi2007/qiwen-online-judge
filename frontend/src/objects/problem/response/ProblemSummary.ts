import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import type { ProblemData } from '@/objects/problem/ProblemData'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSpaceLimitMb } from '@/objects/problem/ProblemSpaceLimitMb'
import type { ProblemTimeLimitMs } from '@/objects/problem/ProblemTimeLimitMs'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'

export type ProblemSummary = AuditFields & {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  data: ProblemData
  ready: boolean
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
  creator: UserIdentity
}
