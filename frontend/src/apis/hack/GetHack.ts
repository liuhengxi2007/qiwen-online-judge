import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { HackId } from '@/objects/hack/HackId'
import { hackIdValue } from '@/objects/hack/HackId'
import type { HackDetail } from '@/objects/hack/response/HackDetail'

export class GetHack implements APIWithSessionMessage<HackDetail> {
  declare readonly responseType?: HackDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(hackId: HackId) {
    this.apiPath = `hacks/${hackIdValue(hackId)}`
  }

  body(): undefined {
    return undefined
  }
}
