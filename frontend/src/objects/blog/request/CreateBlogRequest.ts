import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { ResourceVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'

/** 创建博客请求体；包含标题、正文和初始可见性策略。 */
export type CreateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibilityPolicy: ResourceVisibilityPolicy
}
