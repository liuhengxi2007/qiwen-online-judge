import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataUploadResult } from '@/objects/problem/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 上传题目数据单文件；输入题目 slug、文件、目标路径和可选比赛上下文，使用 multipart 提交。 */
export class UploadProblemDataFile implements APIWithSessionMessage<ProblemDataUploadResult> {
  declare readonly responseType?: ProblemDataUploadResult
  readonly method = 'POST'
  readonly apiPath: string
  private readonly file: File
  private readonly path: ProblemDataFilename | ProblemDataPath

  constructor(problemSlug: ProblemSlug, file: File, path: ProblemDataFilename | ProblemDataPath, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files${params.size > 0 ? `?${params.toString()}` : ''}`
    this.file = file
    this.path = path
  }

  body(): undefined {
    return undefined
  }

  formData(): FormData {
    const formData = new FormData()
    formData.set('file', this.file)
    formData.set('path', this.path)
    return formData
  }
}
