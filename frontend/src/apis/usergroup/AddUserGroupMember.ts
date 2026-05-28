import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { AddUserGroupMemberRequest } from '@/objects/usergroup/request/AddUserGroupMemberRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'

export class AddUserGroupMember implements APIWithSessionMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: AddUserGroupMemberRequest

  constructor(userGroupSlug: UserGroupSlug, request: AddUserGroupMemberRequest) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}/members`
    this.request = request
  }

  body(): AddUserGroupMemberRequest {
    return this.request
  }
}
