import type { BlogSummary } from '@/features/blog/http/response/BlogSummary'
import type { PageResponse } from '@/shared/model/Pagination'

export type BlogListResponse = PageResponse<BlogSummary>
