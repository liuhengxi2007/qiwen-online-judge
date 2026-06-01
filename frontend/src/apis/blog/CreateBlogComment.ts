import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { CreateBlogCommentRequest } from '@/objects/blog/request/CreateBlogCommentRequest'
import { fromBlogDetailContract } from '@/objects/blog/response/BlogDetail'

export class CreateBlogComment implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly decode = fromBlogDetailContract
  readonly apiPath: string
  private readonly request: CreateBlogCommentRequest

  constructor(blogId: BlogId, request: CreateBlogCommentRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/comments`
    this.request = request
  }

  body(): CreateBlogCommentRequest {
    return this.request
  }
}
