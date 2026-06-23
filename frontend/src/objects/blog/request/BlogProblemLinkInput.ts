import type { BlogId } from '@/objects/blog/BlogId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'

/** 题目与博客关联操作的输入；由题目 slug 和博客 ID 路径参数组成。 */
export type BlogProblemLinkInput = {
  problemSlug: ProblemSlug
  blogId: BlogId
}
