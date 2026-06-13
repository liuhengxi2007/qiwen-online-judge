import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/objects/submission/response/SubmissionListResponse'

/** 查询全站提交列表；输入过滤/排序/分页条件，输出提交摘要分页响应。 */
export class ListSubmissions implements APIWithSessionMessage<SubmissionListResponse> {
  declare readonly responseType?: SubmissionListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(request: SubmissionListRequest) {
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
    this.apiPath = `submissions?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
