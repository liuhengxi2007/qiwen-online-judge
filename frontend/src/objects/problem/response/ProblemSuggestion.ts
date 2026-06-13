import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

/** 题目搜索建议；只暴露用于选择的 slug 和标题。 */
export type ProblemSuggestion = {
  slug: ProblemSlug
  title: ProblemTitle
}
