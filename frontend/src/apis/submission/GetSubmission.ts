import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import { fromSubmissionDetailContract } from '@/objects/submission/response/SubmissionDetail'

export class GetSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'GET'
  readonly decode = fromSubmissionDetailContract
  readonly apiPath: string

  constructor(submissionId: SubmissionId) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}`
  }

  body(): undefined {
    return undefined
  }
}
