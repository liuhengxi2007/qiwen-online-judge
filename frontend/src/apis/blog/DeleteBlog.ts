import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

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
