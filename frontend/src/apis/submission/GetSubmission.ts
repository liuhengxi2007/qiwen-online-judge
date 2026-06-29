import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'

/** 获取提交详情；输入提交 ID，输出源码、状态和判题结果。 */
export class GetSubmission implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(submissionId: SubmissionId) {
    this.apiPath = `submissions/${submissionIdValue(submissionId)}`
  }

  body(): undefined {
    return undefined
  }
}
