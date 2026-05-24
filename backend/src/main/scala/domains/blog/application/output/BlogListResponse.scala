package domains.blog.application.output


import shared.model.PageResponse

type BlogListResponse = PageResponse[BlogSummary]
