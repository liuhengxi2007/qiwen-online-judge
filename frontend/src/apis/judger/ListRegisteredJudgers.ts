import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { RegisteredJudgerListItem } from '@/objects/judger/response/RegisteredJudgerListItem'

/** 查询已注册 judger；无请求体，输出当前可见 worker 列表，需管理会话。 */
export class ListRegisteredJudgers implements APIWithSessionMessage<RegisteredJudgerListItem[]> {
  declare readonly responseType?: RegisteredJudgerListItem[]
  readonly method = 'GET'
  readonly apiPath = 'judgers'

  body(): undefined {
    return undefined
  }
}
