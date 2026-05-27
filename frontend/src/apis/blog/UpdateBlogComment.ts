import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { UpdateBlogCommentRequest } from '@/objects/blog/request/UpdateBlogCommentRequest'
import { blogIdValue } from '@/objects/blog/BlogId'
import {
  fromBlogDetailContract,
  toUpdateBlogCommentRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'

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
