import type { APIMessage } from '@/system/api/api-message'
import type { ResolveUserGroupSlugResponse } from '@/objects/usergroup/response/ResolveUserGroupSlugResponse'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'

/** 内部解析用户组 slug 的请求体；用于创建表单可用性检查。 */
type ResolveUserGroupSlugBody = {
  slug: UserGroupSlug
}

/** 检查用户组 slug 是否存在；输入候选 slug，输出占用状态。 */
export class ResolveUserGroupSlug implements APIMessage<ResolveUserGroupSlugResponse> {
  declare readonly responseType?: ResolveUserGroupSlugResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/user-groups/resolve-slug'
  private readonly slug: UserGroupSlug

  constructor(slug: UserGroupSlug) {
    this.slug = slug
  }

  body(): ResolveUserGroupSlugBody {
    return { slug: this.slug }
  }
}
