import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'

export class CreateSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath = 'submissions'
  private readonly request: CreateSubmissionRequest

  constructor(request: CreateSubmissionRequest) {
    this.request = request
  }

  body(): CreateSubmissionRequest {
    return this.request
  }
}
