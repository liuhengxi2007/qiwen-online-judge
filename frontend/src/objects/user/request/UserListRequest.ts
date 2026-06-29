import type { UserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import type { PageRequest } from '@/objects/shared/PageRequest'

/** 用户管理列表请求；可选搜索词配合分页参数，由 API class 编码为查询串。 */
export type UserListRequest = {
  query: UserSearchQuery | null
  pageRequest: PageRequest
}
