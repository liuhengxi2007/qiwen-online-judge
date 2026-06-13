import type { SubmissionHackAvailability } from '@/objects/hack/response/SubmissionHackAvailability'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 查询提交可 Hack 状态；输入提交 ID，输出各子任务可用性。 */
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
