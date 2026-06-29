import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogVote } from '@/objects/blog/BlogVote'

/** 博客评论摘要；包含嵌套关系、投票状态和审计时间。 */
export type BlogCommentSummary = {
  id: BlogCommentId
  parentId: BlogCommentId | null
  content: BlogCommentContent
  author: UserIdentity
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}
