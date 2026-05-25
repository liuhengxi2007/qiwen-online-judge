import type { BlogDetail } from '@/features/blog/model/response/BlogDetail'
import { blogIdValue } from '@/features/blog/lib/blog-parsers'
import { fromBlogDetailContract } from '@/features/blog/http/codec/BlogHttpCodecs'
import type { BlogId } from '@/features/blog/model/BlogId'
import { requestJson } from '@/shared/api/http-client'

export async function getBlog(blogId: BlogId): Promise<BlogDetail> {
  return requestJson(`/api/blogs/${blogIdValue(blogId)}`, fromBlogDetailContract)
}
