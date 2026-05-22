import type { BlogCommentId, BlogDetail, BlogId, CreateBlogCommentRequest } from '@/features/blog/domain/blog'
import {
  blogCommentIdValue,
  blogIdValue,
  fromBlogDetailContract,
  toCreateBlogCommentRequestContract,
} from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export function createBlogCommentReply(
  blogId: BlogId,
  parentCommentId: BlogCommentId,
  request: CreateBlogCommentRequest,
): Promise<BlogDetail> {
  return postJson(
    `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(parentCommentId)}/replies`,
    fromBlogDetailContract,
    toCreateBlogCommentRequestContract(request),
  )
}
