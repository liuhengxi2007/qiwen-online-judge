import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

/** 题目轻量引用；用于博客关联、解析结果等无需完整题面的数据场景。 */
export type ProblemReference = {
  id: ProblemId
  slug: ProblemSlug
  title: ProblemTitle
}
