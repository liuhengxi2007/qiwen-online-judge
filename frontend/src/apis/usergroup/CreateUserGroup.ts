import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateUserGroupRequest } from '@/objects/usergroup/request/CreateUserGroupRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'

/** 创建用户组；输入 slug/name/description，输出新用户组详情，owner 由会话决定。 */
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
