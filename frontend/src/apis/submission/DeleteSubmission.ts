import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 删除提交；输入提交 ID，输出通用成功响应，权限由后端校验。 */
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
