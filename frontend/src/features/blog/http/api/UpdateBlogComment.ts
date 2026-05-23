import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import type { UpdateBlogCommentRequest } from '@/features/blog/http/request/UpdateBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toUpdateBlogCommentRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/model/BlogId'
import { postJson } from '@/shared/api/http-client'

export async function updateBlogComment(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: UpdateBlogCommentRequest,
): Promise<BlogDetail> {
  return postJson(
    `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/update`,
    fromBlogDetailContract,
    toUpdateBlogCommentRequestContract(request),
  )
}
