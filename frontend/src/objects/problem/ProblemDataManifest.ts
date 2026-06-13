import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemDataManifestEntry } from '@/objects/problem/ProblemDataManifestEntry'

/** 判题侧题目数据清单；描述可下载数据文件、版本和所属题目。 */
export type ProblemDataManifest = {
  problemSlug: ProblemSlug
  entries: ProblemDataManifestEntry[]
  version: string
}
