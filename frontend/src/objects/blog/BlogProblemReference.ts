import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

/** 博客关联题目引用；用于展示博客和题目的关联关系。 */
export type BlogProblemReference = {
  slug: ProblemSlug
  title: ProblemTitle
}
