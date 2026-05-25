import type { BlogCommentId } from '@/features/blog/model/BlogCommentId'
import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import { blogCommentIdValue, blogIdValue } from '@/features/blog/lib/blog-parsers'
import { fromBlogDetailContract } from '@/features/blog/http/codec/BlogHttpCodecs'
import type { BlogId } from '@/features/blog/model/BlogId'
import { postJson } from '@/shared/api/http-client'

export async function deleteBlogComment(blogId: BlogId, commentId: BlogCommentId): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/delete`, fromBlogDetailContract, {})
}
