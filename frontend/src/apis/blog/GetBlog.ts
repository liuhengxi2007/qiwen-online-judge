import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { blogIdValue } from '@/objects/blog/blog-parsers'
import { fromBlogDetailContract } from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { requestJson } from '@/system/api/http-client'

export async function getBlog(blogId: BlogId): Promise<BlogDetail> {
  return requestJson(`/api/blogs/${blogIdValue(blogId)}`, fromBlogDetailContract)
}
