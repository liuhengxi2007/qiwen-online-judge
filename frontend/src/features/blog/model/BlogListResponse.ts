import type { BlogSummary } from '@/features/blog/model/BlogSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type BlogListResponse = PageResponse<BlogSummary>
