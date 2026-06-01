import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UpdateUserGroupMemberRoleRequest } from '@/objects/usergroup/request/UpdateUserGroupMemberRoleRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

export class UpdateUserGroupMemberRole implements APIWithSessionMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateUserGroupMemberRoleRequest

  constructor(userGroupSlug: UserGroupSlug, targetUsername: Username, request: UpdateUserGroupMemberRoleRequest) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}/members/${usernameValue(targetUsername)}/role`
    this.request = request
  }

  body(): UpdateUserGroupMemberRoleRequest {
    return this.request
  }
}
