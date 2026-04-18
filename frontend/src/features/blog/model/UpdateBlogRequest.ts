import type { BlogContent } from '@/features/blog/model/BlogContent'
import type { BlogTitle } from '@/features/blog/model/BlogTitle'
import type { BlogType } from '@/features/blog/model/BlogType'
import type { BlogVisibility } from '@/features/blog/model/BlogVisibility'
import type { ProblemSlug } from '@/features/problem/domain/problem'

export type UpdateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibility: BlogVisibility
  blogType: BlogType
  problemSlug: ProblemSlug | null
}
