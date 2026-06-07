import type { SubmissionHackAvailability } from '@/objects/hack/response/SubmissionHackAvailability'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class GetSubmissionHackAvailability implements APIWithSessionMessage<SubmissionHackAvailability> {
  declare readonly responseType?: SubmissionHackAvailability
  readonly method = 'GET'
  readonly apiPath: string

  constructor(submissionId: SubmissionId) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}/hack/availability`
  }

  body(): undefined {
    return undefined
  }
}
