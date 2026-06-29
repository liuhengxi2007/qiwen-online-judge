import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'

/** 更新博客评论请求体；目标评论由 API path 指定。 */
export type UpdateBlogCommentRequest = {
  content: BlogCommentContent
}
