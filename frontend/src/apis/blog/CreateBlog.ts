import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { CreateBlogRequest } from '@/objects/blog/request/CreateBlogRequest'
import {
  fromBlogSummaryContract,
  toCreateBlogRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function createBlog(request: CreateBlogRequest): Promise<BlogSummary> {
  return postJson('/api/blogs', fromBlogSummaryContract, toCreateBlogRequestContract(request))
}
