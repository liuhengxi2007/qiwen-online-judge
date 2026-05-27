import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogProblemReference } from '@/objects/blog/BlogProblemReference'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import type { BlogVote } from '@/objects/blog/BlogVote'

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
