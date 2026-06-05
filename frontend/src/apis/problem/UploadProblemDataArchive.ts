import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDataUploadResult } from '@/objects/problem/response/ProblemDataUploadResult'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class UploadProblemDataArchive implements APIWithSessionMessage<ProblemDataUploadResult> {
  declare readonly responseType?: ProblemDataUploadResult
  readonly method = 'POST'
  readonly apiPath: string
  private readonly file: File

  constructor(problemSlug: ProblemSlug, file: File, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/archive-imports${params.size > 0 ? `?${params.toString()}` : ''}`
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
