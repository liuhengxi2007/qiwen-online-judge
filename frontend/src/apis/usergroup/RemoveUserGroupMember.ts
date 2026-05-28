import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class RemoveUserGroupMember implements APIWithSessionMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(userGroupSlug: UserGroupSlug, targetUsername: Username) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/remove`
  }

  body(): undefined {
    return undefined
  }
}
