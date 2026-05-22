import type { UserGroupListResponse } from '@/features/usergroup/domain/usergroup'
import { fromUserGroupListResponseContract } from '@/features/usergroup/http/codec'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

export async function listUserGroups(pageRequest?: PageRequest): Promise<UserGroupListResponse> {
  const url = new URL('/api/user-groups', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromUserGroupListResponseContract)
}
