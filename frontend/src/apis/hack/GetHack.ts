import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { HackId } from '@/objects/hack/HackId'
import { hackIdValue } from '@/objects/hack/HackId'
import type { HackDetail } from '@/objects/hack/response/HackDetail'

/** 获取 Hack 详情；输入 Hack ID，输出目标提交、输入和判定结果。 */
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
