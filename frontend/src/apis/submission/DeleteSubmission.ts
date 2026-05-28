import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export class DeleteSubmission implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(submissionId: SubmissionId) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
