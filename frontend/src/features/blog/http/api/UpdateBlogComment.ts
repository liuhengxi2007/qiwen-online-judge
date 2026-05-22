import type {
  BlogCommentId,
  BlogDetail,
  UpdateBlogCommentRequest,
} from '@/features/blog/domain/blog'
import {
  blogCommentIdValue,
  blogIdValue,
} from '@/features/blog/domain/blog'
import {
  fromBlogDetailContract,
  toUpdateBlogCommentRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/domain/blog'
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
