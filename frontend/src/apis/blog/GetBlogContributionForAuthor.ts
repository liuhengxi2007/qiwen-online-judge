import type { APIMessage } from '@/system/api/api-message'
import type { BlogContributionResponse } from '@/objects/blog/response/BlogContributionResponse'
import type { Username } from '@/objects/user/Username'

type GetBlogContributionForAuthorBody = {
  username: Username
}

export class GetBlogContributionForAuthor implements APIMessage<BlogContributionResponse> {
  declare readonly responseType?: BlogContributionResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/blogs/contribution-for-author'
  private readonly username: Username

  constructor(username: Username) {
    this.username = username
  }

  body(): GetBlogContributionForAuthorBody {
    return { username: this.username }
  }
}
