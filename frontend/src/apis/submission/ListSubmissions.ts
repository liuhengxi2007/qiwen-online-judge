import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/objects/submission/response/SubmissionListResponse'
import {
  fromSubmissionListResponseContract,
  toSubmissionListRequestContract,
} from '@/apis/submission/codecs/SubmissionHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function listSubmissions(request: SubmissionListRequest): Promise<SubmissionListResponse> {
  const url = new URL('/api/submissions', window.location.origin)
  const contractRequest = toSubmissionListRequestContract(request)

  if (contractRequest.userQuery !== null && contractRequest.userQuery.trim()) {
    url.searchParams.set('username', contractRequest.userQuery)
  }
  if (contractRequest.problemQuery !== null && contractRequest.problemQuery.trim()) {
    url.searchParams.set('problem', contractRequest.problemQuery)
  }
  url.searchParams.set('verdict', contractRequest.verdict)
  url.searchParams.set('sort', contractRequest.sort)
  url.searchParams.set('direction', contractRequest.direction)
  url.searchParams.set('page', String(contractRequest.page))
  url.searchParams.set('pageSize', String(contractRequest.pageSize))

  return requestJson(url.pathname + url.search, fromSubmissionListResponseContract)
}
