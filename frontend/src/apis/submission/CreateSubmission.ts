import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateSubmissionRequest } from '@/objects/submission/request/CreateSubmissionRequest'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
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

/** multipart 提交中的程序元数据；sourcePart 指向 FormData 中对应源码字段。 */
/** 注意：role 和 sourcePart 是动态 multipart 协议字段，后端按字符串校验去重，不是固定状态枚举。 */
export type CreateSubmissionMultipartProgram = {
  role: string
  language: SubmissionLanguage
  sourcePart: string
}

/** multipart 提交中的源码片段；source 可为文本或文件。 */
export type CreateSubmissionMultipartSource = {
  sourcePart: string
  source: string | File
}

/** multipart 创建提交请求；包含题目 slug、程序元数据和源码片段集合。 */
export type CreateSubmissionMultipartRequest = {
  problemSlug: ProblemSlug
  programs: CreateSubmissionMultipartProgram[]
  sources: CreateSubmissionMultipartSource[]
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
