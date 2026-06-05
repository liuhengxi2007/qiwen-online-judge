import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestRegistrationStatus } from '@/objects/contest/response/ContestRegistrationStatus'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class RegisterContest implements APIWithSessionMessage<ContestRegistrationStatus> {
  declare readonly responseType?: ContestRegistrationStatus
  readonly method = 'POST'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/register`
  }

  body(): undefined {
    return undefined
  }
}
