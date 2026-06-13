import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { BlogCommentSummary } from '@/objects/blog/response/BlogCommentSummary'
import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogProblemReference } from '@/objects/blog/BlogProblemReference'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import type { BlogVote } from '@/objects/blog/BlogVote'

/** 博客详情响应；包含正文、关联题目、当前用户投票和评论列表。 */
export type BlogDetail = {
  id: BlogId
  title: BlogTitle
  content: BlogContent
  author: UserIdentity
  visibility: BlogVisibility
  relatedProblems: BlogProblemReference[]
  score: number
  viewerVote: BlogVote | null
  comments: BlogCommentSummary[]
  createdAt: string
  updatedAt: string
}
