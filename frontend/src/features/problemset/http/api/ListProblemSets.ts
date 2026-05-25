import type { ProblemSetListResponse } from '@/features/problemset/http/response/ProblemSetListResponse'
import { fromProblemSetListResponseContract } from '@/features/problemset/http/codec/ProblemSetHttpCodecs'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

export async function listProblemSets(pageRequest?: PageRequest): Promise<ProblemSetListResponse> {
  const url = new URL('/api/problem-sets', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromProblemSetListResponseContract)
}
