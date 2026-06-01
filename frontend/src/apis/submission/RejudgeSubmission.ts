import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { fromSubmissionDetailContract } from '@/objects/submission/response/SubmissionDetail'

export class RejudgeSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly decode = fromSubmissionDetailContract
  readonly apiPath: string

  constructor(submissionId: SubmissionId) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}/rejudge`
  }

  body(): undefined {
    return undefined
  }
}
