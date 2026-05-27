import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { CreateBlogCommentRequest } from '@/objects/blog/request/CreateBlogCommentRequest'
import { blogCommentIdValue, blogIdValue } from '@/objects/blog/blog-parsers'
import {
  fromBlogDetailContract,
  toCreateBlogCommentRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'

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
