import type { APIMessage } from '@/system/api/api-message'
import type { ResolveUserGroupSlugResponse } from '@/objects/usergroup/response/ResolveUserGroupSlugResponse'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'

type ResolveUserGroupSlugBody = {
  slug: UserGroupSlug
}

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
