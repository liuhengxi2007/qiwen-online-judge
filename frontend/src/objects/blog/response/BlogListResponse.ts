import type { BlogSummary } from '@/objects/blog/response/BlogSummary'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 博客列表分页响应；条目为博客摘要。 */
export type BlogListResponse = PageResponse<BlogSummary>
