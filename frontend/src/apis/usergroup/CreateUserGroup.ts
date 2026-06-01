import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateUserGroupRequest } from '@/objects/usergroup/request/CreateUserGroupRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'

export class CreateUserGroup implements APIWithSessionMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'POST'
  readonly apiPath = 'user-groups'
  private readonly request: CreateUserGroupRequest

  constructor(request: CreateUserGroupRequest) {
    this.request = request
  }

  body(): CreateUserGroupRequest {
    return this.request
  }
}
