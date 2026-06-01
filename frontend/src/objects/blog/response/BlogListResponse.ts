import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

export type BlogListResponse = PageResponse<BlogSummary>