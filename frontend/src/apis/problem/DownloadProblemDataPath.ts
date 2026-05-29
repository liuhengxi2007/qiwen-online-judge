import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import { problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class DownloadProblemDataPath implements APIWithSessionMessage<Blob> {
  declare readonly responseType?: Blob
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, path: ProblemDataPath) {
    const params = new URLSearchParams()
    params.set('path', problemDataPathValue(path))
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files/download?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }

  downloadUrl(): string {
    return apiPath(this)
  }
}
