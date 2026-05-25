import type { UserListRequest } from '@/features/user/http/request/UserListRequest'
import type { UserListResponse } from '@/features/user/http/response/UserListResponse'
import {
  fromUserListResponseContract,
  toUserListRequestContract,
} from '@/features/user/http/codec/UserHttpCodecs'
import { requestJson } from '@/shared/api/http-client'

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
