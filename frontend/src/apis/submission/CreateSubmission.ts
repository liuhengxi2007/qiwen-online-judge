import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { CreateSubmissionMultipartRequest } from '@/objects/submission/request/CreateSubmissionMultipartRequest'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'

/** 创建普通提交；输入题目 slug 和程序源码请求，输出提交详情。 */
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

/** 创建普通 multipart 提交；formData 会序列化程序元数据并附加源码文件/文本。 */
export class CreateSubmissionMultipart implements APIWithSessionMessage<SubmissionDetail> {
  declare readonly responseType?: SubmissionDetail
  readonly method = 'POST'
  readonly apiPath = 'submissions'
  private readonly request: CreateSubmissionMultipartRequest

  constructor(request: CreateSubmissionMultipartRequest) {
    this.request = request
  }

  body(): undefined {
    return undefined
  }

  formData(): FormData {
    const formData = new FormData()
    formData.set('problemSlug', problemSlugValue(this.request.problemSlug))
    formData.set('programs', JSON.stringify(this.request.programs))
    for (const source of this.request.sources) {
      formData.set(source.sourcePart, source.source)
    }
    return formData
  }
}
