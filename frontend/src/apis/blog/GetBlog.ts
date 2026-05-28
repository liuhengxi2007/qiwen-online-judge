import type { APIMessage } from '@/system/api/api-message'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'

export class GetBlog implements APIMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(blogId: BlogId) {
    this.apiPath = `blogs/${blogIdValue(blogId)}`
  }

  body(): undefined {
    return undefined
  }
}
