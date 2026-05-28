import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class DownloadProblemData implements APIWithSessionMessage<Blob> {
  declare readonly responseType?: Blob
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, filename: ProblemDataFilename) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/${encodeURIComponent(filename)}`
  }

  body(): undefined {
    return undefined
  }

  downloadUrl(): string {
    return apiPath(this)
  }
}
