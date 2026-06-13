import { apiPath, type APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 下载题目数据归档；输入题目 slug 和可选比赛上下文，输出 Blob 或下载 URL。 */
export class DownloadProblemDataArchive implements APIWithSessionMessage<Blob> {
  declare readonly responseType?: Blob
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/archive-downloads${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }

  downloadUrl(): string {
    return apiPath(this)
  }
}
