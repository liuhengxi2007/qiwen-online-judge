import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UpdateUserGroupRequest } from '@/objects/usergroup/request/UpdateUserGroupRequest'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'

/** 更新用户组基础信息；输入用户组 slug 和更新请求，输出更新后的详情。 */
export class UpdateUserGroup implements APIWithSessionMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateUserGroupRequest

  constructor(userGroupSlug: UserGroupSlug, request: UpdateUserGroupRequest) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}`
    this.request = request
  }

  body(): UpdateUserGroupRequest {
    return this.request
  }
}
