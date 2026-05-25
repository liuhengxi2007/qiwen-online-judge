import type { BlogSummary } from '@/features/blog/model/response/BlogSummary'
import type { CreateBlogRequest } from '@/features/blog/model/request/CreateBlogRequest'
import {
  fromBlogSummaryContract,
  toCreateBlogRequestContract,
} from '@/features/blog/http/codec/BlogHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function createBlog(request: CreateBlogRequest): Promise<BlogSummary> {
  return postJson('/api/blogs', fromBlogSummaryContract, toCreateBlogRequestContract(request))
}
