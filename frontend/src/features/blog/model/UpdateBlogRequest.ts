import type { BlogContent } from '@/features/blog/model/BlogContent'
import type { BlogTitle } from '@/features/blog/model/BlogTitle'
import type { BlogVisibility } from '@/features/blog/model/BlogVisibility'

export type UpdateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibility: BlogVisibility
}
