import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { HackListResponse } from '@/objects/hack/response/HackListResponse'

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
