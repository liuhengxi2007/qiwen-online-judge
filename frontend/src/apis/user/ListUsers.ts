import type { UserListRequest } from '@/objects/user/request/UserListRequest'
import type { UserListResponse } from '@/objects/user/response/UserListResponse'
import {
  fromUserListResponseContract,
  toUserListRequestContract,
} from '@/apis/user/codecs/UserHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function listUsers(request: UserListRequest): Promise<UserListResponse> {
  const url = new URL('/api/users', window.location.origin)
  const contractRequest = toUserListRequestContract(request)
  if (contractRequest.query !== null && contractRequest.query.trim()) {
    url.searchParams.set('q', contractRequest.query)
  }
  url.searchParams.set('page', String(contractRequest.page))
  url.searchParams.set('pageSize', String(contractRequest.pageSize))
  return requestJson(url.pathname + url.search, fromUserListResponseContract)
}
