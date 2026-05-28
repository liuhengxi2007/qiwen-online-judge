import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataUploadResult } from '@/objects/problem/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class UploadProblemDataFile implements APIWithSessionMessage<ProblemDataUploadResult> {
  declare readonly responseType?: ProblemDataUploadResult
  readonly method = 'POST'
  readonly apiPath: string
  private readonly file: File
  private readonly path: ProblemDataFilename | ProblemDataPath

  constructor(problemSlug: ProblemSlug, file: File, path: ProblemDataFilename | ProblemDataPath) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files`
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
