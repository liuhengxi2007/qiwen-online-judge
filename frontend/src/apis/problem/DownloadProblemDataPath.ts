import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import { problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 下载题目数据单一路径；输入题目 slug、数据路径和可选比赛上下文，输出 Blob 或下载 URL。 */
export class DownloadProblemDataPath implements APIWithSessionMessage<Blob> {
  declare readonly responseType?: Blob
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, path: ProblemDataPath, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    params.set('path', problemDataPathValue(path))
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files/download?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }

  downloadUrl(): string {
    return apiPath(this)
  }
}
