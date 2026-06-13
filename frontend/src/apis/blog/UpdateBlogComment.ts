import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { UpdateBlogCommentRequest } from '@/objects/blog/request/UpdateBlogCommentRequest'

/** 更新博客评论；输入博客 ID、评论 ID 和内容，输出更新后的博客详情。 */
export class UpdateBlogComment implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateBlogCommentRequest

  constructor(blogId: BlogId, commentId: BlogCommentId, request: UpdateBlogCommentRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}`
    this.request = request
  }

  body(): UpdateBlogCommentRequest {
    return this.request
  }
}
