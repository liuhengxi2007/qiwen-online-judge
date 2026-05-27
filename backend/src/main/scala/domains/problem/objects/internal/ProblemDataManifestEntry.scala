package domains.problem.objects.internal

import domains.problem.objects.ProblemDataPath

final case class ProblemDataManifestEntry(
  path: ProblemDataPath,
  sizeBytes: Long,
  sha256: String
)
