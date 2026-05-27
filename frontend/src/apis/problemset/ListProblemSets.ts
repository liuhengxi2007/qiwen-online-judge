import type { ProblemSetListResponse } from '@/objects/problemset/response/ProblemSetListResponse'
import { fromProblemSetListResponseContract } from '@/apis/problemset/codecs/ProblemSetHttpCodecs'
import { requestJson } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

export async function listProblemSets(pageRequest?: PageRequest): Promise<ProblemSetListResponse> {
  const url = new URL('/api/problem-sets', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromProblemSetListResponseContract)
}
