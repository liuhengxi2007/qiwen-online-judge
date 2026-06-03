import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { APIMessage } from '@/system/api/api-message'

export class GetContest implements APIMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
