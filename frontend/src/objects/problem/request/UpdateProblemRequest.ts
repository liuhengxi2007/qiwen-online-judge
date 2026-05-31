import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemStatementText } from '@/objects/problem/ProblemStatementText'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

export type UpdateProblemRequest = {
  title: ProblemTitle
  statement: ProblemStatementText
  accessPolicy: ResourceAccessPolicy
  otherUserSubmissionAccess: OtherUserSubmissionAccess
}
