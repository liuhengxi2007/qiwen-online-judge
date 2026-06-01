import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { CreateBlogRequest } from '@/objects/blog/request/CreateBlogRequest'
import { fromBlogSummaryContract } from '@/objects/blog/response/BlogSummary'

export class CreateBlog implements APIWithSessionMessage<BlogSummary> {
  declare readonly responseType?: BlogSummary
  readonly method = 'POST'
  readonly decode = (value: unknown) => fromBlogSummaryContract(value, 'blog summary')
  readonly apiPath = 'blogs'
  private readonly request: CreateBlogRequest

  constructor(request: CreateBlogRequest) {
    this.request = request
  }

  body(): CreateBlogRequest {
    return this.request
  }
}
