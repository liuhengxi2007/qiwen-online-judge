import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import type { VoteBlogCommentRequest } from '@/features/blog/http/request/VoteBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toVoteBlogCommentRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/model/BlogId'
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
