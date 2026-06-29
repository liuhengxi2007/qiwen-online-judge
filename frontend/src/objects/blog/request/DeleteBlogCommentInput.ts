import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'

/** 删除博客评论的输入；由博客 ID 和评论 ID 路径参数组成。 */
export type DeleteBlogCommentInput = {
  blogId: BlogId
  commentId: BlogCommentId
}
