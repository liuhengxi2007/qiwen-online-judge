import type { APIMessage } from '@/system/api/api-message'
import type { UserGroupSlugListResponse } from '@/objects/usergroup/response/UserGroupSlugListResponse'
import type { Username } from '@/objects/user/Username'

/** 内部查询成员所属用户组 slug 的请求体；用户名通过 body 传递。 */
type ListUserGroupSlugsForMemberBody = {
  username: Username
}

/** 查询某用户所在用户组 slug 列表；用于权限策略选择器或内部授权辅助。 */
export class ListUserGroupSlugsForMember implements APIMessage<UserGroupSlugListResponse> {
  declare readonly responseType?: UserGroupSlugListResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/user-groups/member-slugs'
  private readonly username: Username

  constructor(username: Username) {
    this.username = username
  }

  body(): ListUserGroupSlugsForMemberBody {
    return { username: this.username }
  }
}
