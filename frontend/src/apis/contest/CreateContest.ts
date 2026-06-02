import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateContestRequest } from '@/objects/contest/request/CreateContestRequest'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'

export class CreateContest implements APIWithSessionMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'POST'
  readonly apiPath = 'contests'
  private readonly request: CreateContestRequest

  constructor(request: CreateContestRequest) {
    this.request = request
  }

  body(): CreateContestRequest {
    return this.request
  }
}
