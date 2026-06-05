import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/objects/submission/response/SubmissionListResponse'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class ListContestSubmissions implements APIWithSessionMessage<SubmissionListResponse> {
  declare readonly responseType?: SubmissionListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, request: SubmissionListRequest) {
    const params = new URLSearchParams()
    if (request.userQuery !== null && request.userQuery.trim()) {
      params.set('username', request.userQuery)
    }
    if (request.problemQuery !== null && request.problemQuery.trim()) {
      params.set('problem', request.problemQuery)
    }
    params.set('verdict', request.verdict)
    params.set('sort', request.sort)
    params.set('direction', request.direction)
    params.set('page', String(request.pageRequest.page))
    params.set('pageSize', String(request.pageRequest.pageSize))
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/submissions?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
