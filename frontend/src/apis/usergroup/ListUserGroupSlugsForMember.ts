import type { APIMessage } from '@/system/api/api-message'
import type { UserGroupSlugListResponse } from '@/objects/usergroup/response/UserGroupSlugListResponse'
import type { Username } from '@/objects/user/Username'
import { fromUserGroupSlugListResponseContract } from '@/objects/usergroup/response/UserGroupSlugListResponse'

type ListUserGroupSlugsForMemberBody = {
  username: Username
}

export class ListUserGroupSlugsForMember implements APIMessage<UserGroupSlugListResponse> {
  declare readonly responseType?: UserGroupSlugListResponse
  readonly method = 'POST'
  readonly decode = fromUserGroupSlugListResponseContract
  readonly apiPath = 'internal/user-groups/member-slugs'
  private readonly username: Username

  constructor(username: Username) {
    this.username = username
  }

  body(): ListUserGroupSlugsForMemberBody {
    return { username: this.username }
  }
}
