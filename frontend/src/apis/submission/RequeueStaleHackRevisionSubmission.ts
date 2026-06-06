import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionId } from '@/objects/submission/SubmissionId'

export class RequeueStaleHackRevisionSubmission implements APIMessage<boolean> {
  declare readonly responseType?: boolean
  readonly method = 'POST'
  readonly apiPath = 'internal/submissions/judge/requeue-stale-hack-revision'
  private readonly submissionId: SubmissionId

  constructor(submissionId: SubmissionId) {
    this.submissionId = submissionId
  }

  body(): SubmissionId {
    return this.submissionId
  }
}
