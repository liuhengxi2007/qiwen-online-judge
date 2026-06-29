package domains.problem.objects.internal

import domains.problem.objects.ProblemDataPath

/** 题目数据清单文件记录，由 ProblemDataFileTable/ProblemDataApiHelpers 产生，供归档下载、ready 校验和 JudgeTaskBuilder 引用。 */
final case class ProblemDataManifestEntry(
  path: ProblemDataPath,
  sizeBytes: Long,
  sha256: String
)
