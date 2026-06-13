import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 删除博客；输入博客 ID，输出通用成功响应，作者/管理员权限由后端校验。 */
export class DeleteBlog implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(blogId: BlogId) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
