import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { HackSubtaskInfo } from '@/objects/hack/response/HackSubtaskInfo'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'

export class GetSubmissionHackSubtask implements APIWithSessionMessage<HackSubtaskInfo> {
  declare readonly responseType?: HackSubtaskInfo
  readonly method = 'GET'
  readonly apiPath: string

  constructor(submissionId: SubmissionId, subtaskIndex: number) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}/hack/subtasks/${subtaskIndex}`
  }

  body(): undefined {
    return undefined
  }
}
