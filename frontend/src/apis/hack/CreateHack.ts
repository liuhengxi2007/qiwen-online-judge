import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateHackRequest } from '@/objects/hack/request/CreateHackRequest'
import type { HackDetail } from '@/objects/hack/response/HackDetail'

export class CreateHack implements APIWithSessionMessage<HackDetail> {
  declare readonly responseType?: HackDetail
  readonly method = 'POST'
  readonly apiPath = 'hacks'
  private readonly request: CreateHackRequest

  constructor(request: CreateHackRequest) {
    this.request = request
  }

  body(): CreateHackRequest {
    return this.request
  }
}
