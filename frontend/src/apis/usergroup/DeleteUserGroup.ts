import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { userGroupSlugValue } from '@/objects/usergroup/UserGroupSlug'

export class DeleteUserGroup implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(userGroupSlug: UserGroupSlug) {
    this.apiPath = `user-groups/${userGroupSlugValue(userGroupSlug)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
