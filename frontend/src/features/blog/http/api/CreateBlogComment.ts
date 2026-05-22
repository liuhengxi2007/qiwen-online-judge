import type {
  BlogCommentId,
  BlogDetail,
  CreateBlogCommentRequest,
} from '@/features/blog/domain/blog'
import {
  blogCommentIdValue,
  blogIdValue,
  fromBlogDetailContract,
  toCreateBlogCommentRequestContract,
} from '@/features/blog/domain/blog'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function createBlogComment(
  blogId: BlogId,
  request: CreateBlogCommentRequest,
  parentCommentId?: BlogCommentId | null,
): Promise<BlogDetail> {
  const path = parentCommentId
    ? `/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(parentCommentId)}/replies`
    : `/api/blogs/${blogIdValue(blogId)}/comments`
  return postJson(path, fromBlogDetailContract, toCreateBlogCommentRequestContract(request))
}
