import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { UpdateBlogRequest } from '@/objects/blog/request/UpdateBlogRequest'
import { fromBlogDetailContract } from '@/objects/blog/response/BlogDetail'

export class UpdateBlog implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly decode = fromBlogDetailContract
  readonly apiPath: string
  private readonly request: UpdateBlogRequest

  constructor(blogId: BlogId, request: UpdateBlogRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}`
    this.request = request
  }

  body(): UpdateBlogRequest {
    return this.request
  }
}
