import type {
  BlogCommentId,
  BlogDetail,
} from '@/features/blog/domain/blog'
import {
  blogCommentIdValue,
  blogIdValue,
} from '@/features/blog/domain/blog'
import { fromBlogDetailContract } from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function deleteBlogComment(blogId: BlogId, commentId: BlogCommentId): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/delete`, fromBlogDetailContract, {})
}
