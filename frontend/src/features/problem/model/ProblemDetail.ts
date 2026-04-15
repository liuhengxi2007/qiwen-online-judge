import type { Username } from '@/features/auth/model/AuthValues'
import type { OthersSubmissionAccess } from '@/features/problem/model/OthersSubmissionAccess'
import type { ProblemData } from '@/features/problem/model/ProblemData'
import type { ProblemId } from '@/features/problem/model/ProblemId'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemSpaceLimitMb } from '@/features/problem/model/ProblemSpaceLimitMb'
import type { ProblemStatementText } from '@/features/problem/model/ProblemStatementText'
import type { ProblemTimeLimitMs } from '@/features/problem/model/ProblemTimeLimitMs'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'
import type { AuditFields } from '@/shared/model/AuditFields'

export type ProblemDetail = AuditFields & {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  data: ProblemData
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
  creatorUsername: Username
  canManage: boolean
}
