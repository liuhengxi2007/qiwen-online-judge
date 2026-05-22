package domains.blog.http.response

import domains.blog.model.*

import domains.shared.model.PageResponse

type BlogListResponse = PageResponse[BlogSummary]
