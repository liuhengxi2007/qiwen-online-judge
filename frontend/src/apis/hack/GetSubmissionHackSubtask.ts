import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { HackSubtaskInfo } from '@/objects/hack/response/HackSubtaskInfo'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'

/** 查询可 Hack 子任务信息；输入提交 ID 和子任务序号，输出目标详情和模式要求。 */
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
