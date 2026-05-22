import type { OthersSubmissionAccess } from '@/features/problem/model/OthersSubmissionAccess'
import type { ProblemSpaceLimitMb } from '@/features/problem/model/ProblemSpaceLimitMb'
import type { ProblemStatementText } from '@/features/problem/model/ProblemStatementText'
import type { ProblemTimeLimitMs } from '@/features/problem/model/ProblemTimeLimitMs'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'
import type { ResourceAccessPolicy } from '@/shared/access/AccessPolicy'

export type UpdateProblemRequest = {
  title: ProblemTitle
  statement: ProblemStatementText
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
}
