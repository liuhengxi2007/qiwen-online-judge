import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import { blogCommentIdValue } from '@/objects/blog/BlogCommentId'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { blogIdValue } from '@/objects/blog/BlogId'
import { fromBlogDetailContract } from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'

export async function deleteBlogComment(blogId: BlogId, commentId: BlogCommentId): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/comments/${blogCommentIdValue(commentId)}/delete`, fromBlogDetailContract, {})
}
