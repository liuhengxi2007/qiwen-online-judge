import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogVote } from '@/objects/blog/BlogVote'

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
