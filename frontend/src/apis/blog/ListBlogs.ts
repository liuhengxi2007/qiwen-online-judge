import type { APIMessage } from '@/system/api/api-message'
import type { BlogListResponse } from '@/objects/blog/response/BlogListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromBlogListResponseContract } from '@/objects/blog/response/BlogListResponse'

export class ListBlogs implements APIMessage<BlogListResponse> {
  declare readonly responseType?: BlogListResponse
  readonly method = 'GET'
  readonly decode = fromBlogListResponseContract
  readonly apiPath: string

  constructor(authorUsername?: Username | null, pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (authorUsername) {
      params.set('username', usernameValue(authorUsername))
    }
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `blogs${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
