import type { APIMessage } from '@/system/api/api-message'
import type { SubmissionId } from '@/objects/submission/SubmissionId'

/** 内部重新排队过期 Hack 修订提交；输入提交 ID，输出是否重新排队。 */
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
