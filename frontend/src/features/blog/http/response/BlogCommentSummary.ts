import type { UserIdentity } from '@/features/user/domain/user'
import type { BlogCommentContent } from '@/features/blog/model/BlogCommentContent'
import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogVote } from '@/features/blog/model/BlogVote'

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
