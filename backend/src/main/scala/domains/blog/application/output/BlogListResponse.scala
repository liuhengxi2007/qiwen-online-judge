package domains.blog.application.output

import domains.blog.model.*

import domains.shared.model.PageResponse

type BlogListResponse = PageResponse[BlogSummary]
