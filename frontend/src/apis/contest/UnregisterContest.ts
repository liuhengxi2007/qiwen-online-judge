import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestRegistrationStatus } from '@/objects/contest/response/ContestRegistrationStatus'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 取消比赛报名；输入比赛 slug，输出当前会话的报名状态。 */
export class UnregisterContest implements APIWithSessionMessage<ContestRegistrationStatus> {
  declare readonly responseType?: ContestRegistrationStatus
  readonly method = 'POST'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/unregister`
  }

  body(): undefined {
    return undefined
  }
}
