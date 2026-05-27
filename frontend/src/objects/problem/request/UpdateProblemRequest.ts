import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import type { ProblemSpaceLimitMb } from '@/objects/problem/ProblemSpaceLimitMb'
import type { ProblemStatementText } from '@/objects/problem/ProblemStatementText'
import type { ProblemTimeLimitMs } from '@/objects/problem/ProblemTimeLimitMs'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

export type UpdateProblemRequest = {
  title: ProblemTitle
  statement: ProblemStatementText
  timeLimitMs: ProblemTimeLimitMs
  spaceLimitMb: ProblemSpaceLimitMb
  accessPolicy: ResourceAccessPolicy
  othersSubmissionAccess: OthersSubmissionAccess
}
