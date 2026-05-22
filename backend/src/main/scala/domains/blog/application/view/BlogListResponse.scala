package domains.blog.application.view

import domains.blog.model.*

import domains.shared.model.PageResponse

type BlogListResponse = PageResponse[BlogSummary]
