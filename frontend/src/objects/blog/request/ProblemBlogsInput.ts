import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { PageRequest } from '@/objects/shared/PageRequest'

/** 题目博客列表查询输入；包含题目 slug 和分页参数。 */
export type ProblemBlogsInput = {
  problemSlug: ProblemSlug
  pageRequest: PageRequest
}
