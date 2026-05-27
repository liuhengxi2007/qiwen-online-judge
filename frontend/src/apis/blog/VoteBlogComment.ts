import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { VoteBlogCommentRequest } from '@/objects/blog/request/VoteBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/objects/blog/blog-parsers'
import {
  fromBlogDetailContract,
  toVoteBlogCommentRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'

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
