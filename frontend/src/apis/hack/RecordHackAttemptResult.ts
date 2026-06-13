import type { APIMessage } from '@/system/api/api-message'
import type { HackId } from '@/objects/hack/HackId'

/** 内部记录 Hack 判题结果请求体；request 当前透传 worker 原始结果载荷。 */
/** FIXME-CN: request 实际对应 judge protocol 的 ReportHackResultRequest，使用 unknown 会绕过上报结果结构校验。 */
type RecordHackAttemptResultBody = {
  hackId: HackId
  request: unknown
}

/** 记录 Hack 尝试结果的内部 API；输入 Hack ID 和未建模结果载荷，输出后端响应。 */
/** FIXME-CN: 后端响应为 Option[ProblemId]，当前 unknown 让成功 hack 后受影响题目 ID 缺少类型约束。 */
export class RecordHackAttemptResult implements APIMessage<unknown> {
  declare readonly responseType?: unknown
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/result'
  private readonly hackId: HackId
  private readonly request: unknown

  constructor(hackId: HackId, request: unknown) {
    this.hackId = hackId
    this.request = request
  }

  body(): RecordHackAttemptResultBody {
    return { hackId: this.hackId, request: this.request }
  }
}
