import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { UpdateBlogRequest } from '@/objects/blog/request/UpdateBlogRequest'

export class UpdateBlog implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateBlogRequest

  constructor(blogId: BlogId, request: UpdateBlogRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/update`
    this.request = request
  }

  body(): UpdateBlogRequest {
    return this.request
  }
}
