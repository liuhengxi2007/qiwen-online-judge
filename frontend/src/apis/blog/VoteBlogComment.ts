import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { VoteBlogCommentRequest } from '@/objects/blog/request/VoteBlogCommentRequest'

/** 对博客评论投票；输入博客 ID、评论 ID 和投票方向，输出更新后的博客详情。 */
export class VoteBlogComment implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: VoteBlogCommentRequest

  constructor(blogId: BlogId, commentId: BlogCommentId, request: VoteBlogCommentRequest) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/vote`
    this.request = request
  }

  body(): VoteBlogCommentRequest {
    return this.request
  }
}
