import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogDetail } from '@/features/blog/model/response/BlogDetail'
import type { BlogId } from '@/features/blog/model/BlogId'
import type { CreateBlogCommentRequest } from '@/features/blog/model/request/CreateBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toCreateBlogCommentRequestContract,
} from '@/features/blog/http/codec/BlogHttpCodecs'
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
