import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import type { CreateBlogCommentRequest } from '@/features/blog/http/request/CreateBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toCreateBlogCommentRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/model/BlogId'
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
