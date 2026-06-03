import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class RegisterContest implements APIWithSessionMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/register`
  }

  body(): undefined {
    return undefined
  }
}
