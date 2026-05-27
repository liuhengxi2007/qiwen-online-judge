import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogId } from '@/objects/blog/BlogId'
import type { CreateBlogCommentRequest } from '@/objects/blog/request/CreateBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/objects/blog/blog-parsers'
import {
  fromBlogDetailContract,
  toCreateBlogCommentRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
