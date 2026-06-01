import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataUploadResult } from '@/objects/problem/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromProblemDataUploadResultContract } from '@/objects/problem/response/ProblemDataUploadResult'

export class UploadProblemDataArchive implements APIWithSessionMessage<ProblemDataUploadResult> {
  declare readonly responseType?: ProblemDataUploadResult
  readonly method = 'POST'
  readonly decode = fromProblemDataUploadResultContract
  readonly apiPath: string
  private readonly file: File

  constructor(problemSlug: ProblemSlug, file: File) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/archive-imports`
    this.file = file
  }

  body(): undefined {
    return undefined
  }

  formData(): FormData {
    const formData = new FormData()
    formData.set('file', this.file)
    return formData
  }
}
