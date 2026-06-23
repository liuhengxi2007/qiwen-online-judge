import type { BlogId } from '@/objects/blog/BlogId'
import type { VoteBlogRequest } from '@/objects/blog/request/VoteBlogRequest'

/** 博客投票输入；路径参数定位博客，请求体携带投票方向。 */
export type VoteBlogInput = {
  blogId: BlogId
  request: VoteBlogRequest
}
