import type { APIMessage } from '@/system/api/api-message'
import type { HackId } from '@/objects/hack/HackId'

type RecordHackAttemptResultBody = {
  hackId: HackId
  request: unknown
}

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
