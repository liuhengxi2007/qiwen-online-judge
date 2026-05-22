import type { BlogDetail } from '@/features/blog/domain/blog'
import { blogIdValue } from '@/features/blog/domain/blog'
import { fromBlogDetailContract } from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/domain/blog'
import { requestJson } from '@/shared/api/http-client'

export async function getBlog(blogId: BlogId): Promise<BlogDetail> {
  return requestJson(`/api/blogs/${blogIdValue(blogId)}`, fromBlogDetailContract)
}
