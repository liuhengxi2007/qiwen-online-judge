import { blogIdValue } from '@/features/blog/domain/blog'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'
import { decodeSuccessResponse } from '@/shared/api/http-client'

export async function deleteBlog(blogId: BlogId): Promise<void> {
  await postJson(`/api/blogs/${blogIdValue(blogId)}/delete`, decodeSuccessResponse, {})
}
