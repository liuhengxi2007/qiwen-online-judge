import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemStatementText } from '@/objects/problem/ProblemStatementText'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { Username } from '@/objects/user/Username'

/** 更新题目请求体；authorUsername 为空表示清除或不指定作者，权限由后端确认。 */
export type UpdateProblemRequest = {
  title: ProblemTitle
  statement: ProblemStatementText
  accessPolicy: ResourceAccessPolicy
  otherUserSubmissionAccess: OtherUserSubmissionAccess
  authorUsername: Username | null
}
