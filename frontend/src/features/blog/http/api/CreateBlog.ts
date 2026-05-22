import type {
  BlogSummary,
  CreateBlogRequest,
} from '@/features/blog/domain/blog'
import {
  fromBlogSummaryContract,
  toCreateBlogRequestContract,
} from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function createBlog(request: CreateBlogRequest): Promise<BlogSummary> {
  return postJson('/api/blogs', fromBlogSummaryContract, toCreateBlogRequestContract(request))
}
