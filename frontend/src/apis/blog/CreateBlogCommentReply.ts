import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { CreateBlogCommentRequest } from '@/objects/blog/request/CreateBlogCommentRequest'

/** 创建博客评论回复；输入博客 ID、父评论 ID 和内容，输出更新后的博客详情。 */
export class CreateBlogCommentReply implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: CreateBlogCommentRequest

  constructor(blogId: BlogId, parentCommentId: BlogCommentId, request: CreateBlogCommentRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(parentCommentId)}/replies`
    this.request = request
  }

  body(): CreateBlogCommentRequest {
    return this.request
  }
}
