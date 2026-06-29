import type { APIMessage } from '@/system/api/api-message'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { UserGroupListResponse } from '@/objects/usergroup/response/UserGroupListResponse'

/** 查询用户组列表；可选分页参数，输出用户组摘要分页响应。 */
export class ListUserGroups implements APIMessage<UserGroupListResponse> {
  declare readonly responseType?: UserGroupListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `user-groups${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
