import type { APIMessage } from '@/system/api/api-message'
import type { HackId } from '@/objects/hack/HackId'
import type { ProblemId } from '@/objects/problem/ProblemId'
import { parseProblemId } from '@/objects/problem/ProblemId'
import type { ReportHackResultRequest } from '@/objects/hack/request/ReportHackResultRequest'

/** 内部记录 Hack 判题结果请求体；request 对应 judge protocol 的 ReportHackResultRequest。 */
type RecordHackAttemptResultBody = {
  hackId: HackId
  request: ReportHackResultRequest
}

/** 记录 Hack 尝试结果的内部 API；成功 hack 时返回受影响题目 ID，否则为空。 */
export class RecordHackAttemptResult implements APIMessage<ProblemId | null> {
  declare readonly responseType?: ProblemId | null
  readonly method = 'POST'
  readonly apiPath = 'internal/hacks/judge/result'
  readonly decodeResponse = decodeRecordHackAttemptResultResponse
  private readonly hackId: HackId
  private readonly request: ReportHackResultRequest

  constructor(hackId: HackId, request: ReportHackResultRequest) {
    this.hackId = hackId
    this.request = request
  }

  body(): RecordHackAttemptResultBody {
    return { hackId: this.hackId, request: this.request }
  }
}

function decodeRecordHackAttemptResultResponse(value: unknown): ProblemId | null {
  if (value === null) {
    return null
  }
  if (typeof value !== 'string') {
    throw new Error('Invalid hack result response payload.')
  }

  const parsed = parseProblemId(value)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }

  return parsed.value
}
