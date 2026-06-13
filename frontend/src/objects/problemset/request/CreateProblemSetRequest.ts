import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

/** 创建题集请求体；包含初始访问策略但不包含题目列表。 */
export type CreateProblemSetRequest = {
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
}
