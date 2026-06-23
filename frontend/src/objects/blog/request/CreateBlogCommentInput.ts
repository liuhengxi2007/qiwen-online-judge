import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { CreateBlogCommentRequest } from '@/objects/blog/request/CreateBlogCommentRequest'

/** 创建博客评论的输入；顶层评论没有 parentCommentId，回复会携带父评论 ID。 */
export type CreateBlogCommentInput = {
  blogId: BlogId
  parentCommentId: BlogCommentId | null
  request: CreateBlogCommentRequest
}
