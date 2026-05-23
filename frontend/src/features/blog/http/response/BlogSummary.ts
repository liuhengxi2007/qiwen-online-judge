import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { BlogContent } from '@/features/blog/model/BlogContent'
import type { BlogId } from '@/features/blog/model/BlogId'
import type { BlogProblemReference } from '@/features/blog/model/BlogProblemReference'
import type { BlogTitle } from '@/features/blog/model/BlogTitle'
import type { BlogVisibility } from '@/features/blog/model/BlogVisibility'
import type { BlogVote } from '@/features/blog/model/BlogVote'

export type BlogSummary = {
  id: BlogId
  title: BlogTitle
  content: BlogContent
  author: UserIdentity
  visibility: BlogVisibility
  relatedProblems: BlogProblemReference[]
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}
