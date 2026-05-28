import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'

export class DeleteBlogComment implements APIWithSessionMessage<BlogDetail> {
  declare readonly responseType?: BlogDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(blogId: BlogId, commentId: BlogCommentId) {
    this.apiPath = `blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
