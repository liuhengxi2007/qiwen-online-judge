import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'

/** 创建博客评论请求体；目博客或父评论由 API path 指定。 */
export type CreateBlogCommentRequest = {
  content: BlogCommentContent
}
