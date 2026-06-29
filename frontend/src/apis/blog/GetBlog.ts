import type { APIMessage } from '@/system/api/api-message'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'

/** 获取博客详情；输入博客 ID，输出正文、评论和投票状态。 */
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
