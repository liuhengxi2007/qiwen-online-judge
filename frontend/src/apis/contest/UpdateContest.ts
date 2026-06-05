import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { UpdateContestRequest } from '@/objects/contest/request/UpdateContestRequest'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class UpdateContest implements APIWithSessionMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateContestRequest

  constructor(contestSlug: ContestSlug, request: UpdateContestRequest) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/update`
    this.request = request
  }

  body(): UpdateContestRequest {
    return this.request
  }
}
