import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import { fromBlogSummaryContract } from '@/objects/blog/response/BlogSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'

export type BlogListResponse = PageResponse<BlogSummary>

export function fromBlogListResponseContract(value: unknown, label = 'blog list response'): BlogListResponse {
  return fromPageResponseContract(value, label, fromBlogSummaryContract)
}
