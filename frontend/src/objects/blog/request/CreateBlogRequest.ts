import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'

/** 创建博客请求体；包含标题、正文和初始可见性。 */
export type CreateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibility: BlogVisibility
}
