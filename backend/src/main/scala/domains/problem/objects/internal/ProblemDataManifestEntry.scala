package domains.problem.objects.internal

import domains.problem.objects.ProblemDataPath

/** 题目数据清单中的单个文件记录；用于判题 worker 校验文件引用和版本。 */
final case class ProblemDataManifestEntry(
  path: ProblemDataPath,
  sizeBytes: Long,
  sha256: String
)
