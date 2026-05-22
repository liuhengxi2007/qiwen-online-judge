import type {
  ProblemListRequest,
  ProblemListResponse,
} from '@/features/problem/domain/problem'
import {
  fromProblemListResponseContract,
  toProblemListRequestContract,
} from '@/features/problem/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function listProblems(request: ProblemListRequest): Promise<ProblemListResponse> {
  const url = new URL('/api/problems', window.location.origin)
  const contractRequest = toProblemListRequestContract(request)
  if (contractRequest.query !== null && contractRequest.query.trim()) {
    url.searchParams.set('q', contractRequest.query)
  }
  url.searchParams.set('page', String(contractRequest.page))
  url.searchParams.set('pageSize', String(contractRequest.pageSize))
  return requestJson(url.pathname + url.search, fromProblemListResponseContract)
}
