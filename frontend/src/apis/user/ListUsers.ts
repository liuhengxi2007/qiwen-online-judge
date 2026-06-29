import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { UserListRequest } from '@/objects/user/request/UserListRequest'
import type { UserListResponse } from '@/objects/user/response/UserListResponse'

/** 查询受管用户列表；输入过滤和分页，输出包含邮箱/权限的管理分页响应。 */
export class ListUsers implements APIWithSessionMessage<UserListResponse> {
  declare readonly responseType?: UserListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(request: UserListRequest) {
    const params = new URLSearchParams()
    if (request.query !== null && request.query.trim()) {
      params.set('q', request.query)
    }
    params.set('page', String(request.pageRequest.page))
    params.set('pageSize', String(request.pageRequest.pageSize))
    this.apiPath = `users?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
