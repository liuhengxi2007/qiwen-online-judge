import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { UpdateBlogCommentRequest } from '@/objects/blog/request/UpdateBlogCommentRequest'

/** 更新博客评论的输入；路径参数定位评论，请求体携带更新内容。 */
export type UpdateBlogCommentInput = {
  blogId: BlogId
  commentId: BlogCommentId
  request: UpdateBlogCommentRequest
}
