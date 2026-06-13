import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'

/** 判题数据清单条目；包含相对路径、文件大小和 sha256 校验值。 */
export type ProblemDataManifestEntry = {
  path: ProblemDataPath
  sizeBytes: number
  sha256: string
}
