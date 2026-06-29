import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'

/** 重新判题提交；输入提交 ID，输出更新后的提交详情。 */
export class RejudgeSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(submissionId: SubmissionId) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}/rejudge`
  }

  body(): undefined {
    return undefined
  }
}
