import type { UserGroupListResponse } from '@/objects/usergroup/response/UserGroupListResponse'
import { fromUserGroupListResponseContract } from '@/apis/usergroup/codecs/UserGroupHttpCodecs'
import { requestJson } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

export async function listUserGroups(pageRequest?: PageRequest): Promise<UserGroupListResponse> {
  const url = new URL('/api/user-groups', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromUserGroupListResponseContract)
}
