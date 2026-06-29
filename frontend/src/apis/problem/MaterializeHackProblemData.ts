import type { APIMessage } from '@/system/api/api-message'
import type { MaterializeHackProblemDataInput } from '@/objects/problem/request/MaterializeHackProblemDataInput'

/** 内部 hack 数据物化 API；frontend mirror only, pages should not call it. */
export class MaterializeHackProblemData implements APIMessage<void> {
  declare readonly responseType?: void
  readonly method = 'POST'
  readonly apiPath = 'internal/problems/hack-data'
  private readonly request: MaterializeHackProblemDataInput

  constructor(request: MaterializeHackProblemDataInput) {
    this.request = request
  }

  body(): MaterializeHackProblemDataInput {
    return this.request
  }
}
