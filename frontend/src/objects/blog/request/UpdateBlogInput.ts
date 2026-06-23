import type { BlogId } from '@/objects/blog/BlogId'
import type { UpdateBlogRequest } from '@/objects/blog/request/UpdateBlogRequest'

/** 更新博客的输入；路径参数定位博客，请求体携带更新内容。 */
export type UpdateBlogInput = {
  blogId: BlogId
  request: UpdateBlogRequest
}
