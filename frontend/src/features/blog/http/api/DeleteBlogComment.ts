import type {
  BlogCommentId,
  BlogDetail,
} from '@/features/blog/domain/blog'
import {
  blogCommentIdValue,
  blogIdValue,
  fromBlogDetailContract,
} from '@/features/blog/domain/blog'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function deleteBlogComment(blogId: BlogId, commentId: BlogCommentId): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/delete`, fromBlogDetailContract, {})
}
