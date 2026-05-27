import { blogIdValue } from '@/objects/blog/blog-parsers'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'
import { decodeSuccessResponse } from '@/system/api/http-client'

export async function deleteBlog(blogId: BlogId): Promise<void> {
  await postJson(`/api/blogs/${blogIdValue(blogId)}/delete`, decodeSuccessResponse, {})
}
