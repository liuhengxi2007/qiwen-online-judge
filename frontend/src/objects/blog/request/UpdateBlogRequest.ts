import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { ResourceVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'

/** 更新博客请求体；允许修改标题、正文和可见性策略。 */
export type UpdateBlogRequest = {
  title: BlogTitle
  content: BlogContent
  visibilityPolicy: ResourceVisibilityPolicy
}
