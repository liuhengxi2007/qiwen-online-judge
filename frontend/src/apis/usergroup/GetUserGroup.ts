import type { APIMessage } from '@/system/api/api-message'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'

/** 获取用户组详情；输入公开 slug，输出用户组详情和成员列表。 */
export class GetUserGroup implements APIMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(userGroupSlug: UserGroupSlug) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
