import type { BlogSummary } from '@/features/blog/model/response/BlogSummary'
import type { PageResponse } from '@/shared/model/PageResponse'

export type BlogListResponse = PageResponse<BlogSummary>
