import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { Username } from '@/objects/user/Username'

/** 更新题集请求体；authorUsername 为空表示清除或不指定作者，权限由后端校验。 */
export type UpdateProblemSetRequest = {
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  authorUsername: Username | null
}
