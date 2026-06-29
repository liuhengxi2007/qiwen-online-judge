import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'

/** 删除博客评论；输入博客 ID 和评论 ID，输出更新后的博客详情。 */
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
