import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { HackListResponse } from '@/objects/hack/response/HackListResponse'

/** 查询 Hack 列表；输入页码和页大小，输出 Hack 摘要分页响应。 */
export class ListHacks implements APIWithSessionMessage<HackListResponse> {
  declare readonly responseType?: HackListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(page: number, pageSize: number) {
    const params = new URLSearchParams()
    params.set('page', String(page))
    params.set('pageSize', String(pageSize))
    this.apiPath = `hacks?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
