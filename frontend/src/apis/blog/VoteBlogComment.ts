import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { VoteBlogCommentRequest } from '@/objects/blog/request/VoteBlogCommentRequest'

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
