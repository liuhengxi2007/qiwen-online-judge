package domains.blog.application.output

import domains.blog.model.*

import shared.model.PageResponse

type BlogListResponse = PageResponse[BlogSummary]
