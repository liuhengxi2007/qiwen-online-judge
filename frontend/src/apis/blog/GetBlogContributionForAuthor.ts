import type { APIMessage } from '@/system/api/api-message'
import type { BlogContributionResponse } from '@/objects/blog/response/BlogContributionResponse'
import type { Username } from '@/objects/user/Username'

/** 内部作者博客贡献请求体；用户名通过 body 传递。 */
type GetBlogContributionForAuthorBody = {
  username: Username
}

/** 查询作者博客贡献；输入用户名，输出后端计算的博客贡献值。 */
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
