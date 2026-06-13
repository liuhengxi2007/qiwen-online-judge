import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'

/** 更新博客请求体；允许修改标题、正文和可见性。 */
export type UpdateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibility: BlogVisibility
}
