import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'

export type CreateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibility: BlogVisibility
}
