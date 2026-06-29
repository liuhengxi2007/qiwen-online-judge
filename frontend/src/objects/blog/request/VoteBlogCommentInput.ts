import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogId } from '@/objects/blog/BlogId'
import type { VoteBlogCommentRequest } from '@/objects/blog/request/VoteBlogCommentRequest'

/** 博客评论投票输入；路径参数定位评论，请求体携带投票方向。 */
export type VoteBlogCommentInput = {
  blogId: BlogId
  commentId: BlogCommentId
  request: VoteBlogCommentRequest
}
