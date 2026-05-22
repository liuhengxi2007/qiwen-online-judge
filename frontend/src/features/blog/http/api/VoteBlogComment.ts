import type {
  BlogCommentId,
  BlogDetail,
  VoteBlogCommentRequest,
} from '@/features/blog/domain/blog'
import {
  blogCommentIdValue,
  blogIdValue,
} from '@/features/blog/domain/blog'
import {
  fromBlogDetailContract,
  toVoteBlogCommentRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function voteBlogComment(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: VoteBlogCommentRequest,
): Promise<BlogDetail> {
  return postJson(
    `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/vote`,
    fromBlogDetailContract,
    toVoteBlogCommentRequestContract(request),
  )
}
