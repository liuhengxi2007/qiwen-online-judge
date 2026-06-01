import type { APIMessage } from '@/system/api/api-message'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'
import { fromUserGroupDetailContract } from '@/objects/usergroup/response/UserGroupDetail'

export class GetUserGroup implements APIMessage<UserGroupDetail> {
  declare readonly responseType?: UserGroupDetail
  readonly method = 'GET'
  readonly decode = fromUserGroupDetailContract
  readonly apiPath: string

  constructor(userGroupSlug: UserGroupSlug) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
