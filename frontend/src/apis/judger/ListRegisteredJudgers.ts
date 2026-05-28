import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { RegisteredJudgerListItem } from '@/objects/judger/response/RegisteredJudgerListItem'

export class ListRegisteredJudgers implements APIWithSessionMessage<RegisteredJudgerListItem[]> {
  declare readonly responseType?: RegisteredJudgerListItem[]
  readonly method = 'GET'
  readonly apiPath = 'judgers'

  body(): undefined {
    return undefined
  }
}
