import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { VoteBlogRequest } from '@/objects/blog/request/VoteBlogRequest'
import { fromBlogDetailContract } from '@/objects/blog/response/BlogDetail'

export class VoteBlog implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly decode = fromBlogDetailContract
  readonly apiPath: string
  private readonly request: VoteBlogRequest

  constructor(blogId: BlogId, request: VoteBlogRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/vote`
    this.request = request
  }

  body(): VoteBlogRequest {
    return this.request
  }
}
