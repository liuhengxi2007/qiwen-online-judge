import type { UserIdentity } from '@/features/auth/domain/auth'
import type { BlogContent } from '@/features/blog/model/BlogContent'
import type { BlogId } from '@/features/blog/model/BlogId'
import type { BlogTitle } from '@/features/blog/model/BlogTitle'
import type { BlogType } from '@/features/blog/model/BlogType'
import type { BlogVisibility } from '@/features/blog/model/BlogVisibility'
import type { BlogVote } from '@/features/blog/model/BlogVote'
import type { ProblemSlug, ProblemTitle } from '@/features/problem/domain/problem'

export type BlogSummary = {
  id: BlogId
  title: BlogTitle
  content: BlogContent
  author: UserIdentity
  visibility: BlogVisibility
  blogType: BlogType
  problemSlug: ProblemSlug | null
  problemTitle: ProblemTitle | null
  score: number
  viewerVote: BlogVote | null
  createdAt: string
  updatedAt: string
}
