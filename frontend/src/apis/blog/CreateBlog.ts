import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { CreateBlogRequest } from '@/objects/blog/request/CreateBlogRequest'

/** 创建博客；输入标题、正文和可见性，输出新博客摘要。 */
export class CreateBlog implements APIWithSessionMessage<BlogSummary> {
  declare readonly responseType?: BlogSummary
  readonly method = 'POST'
  readonly apiPath = 'blogs'
  private readonly request: CreateBlogRequest

  constructor(request: CreateBlogRequest) {
    this.request = request
  }

  body(): CreateBlogRequest {
    return this.request
  }
}
