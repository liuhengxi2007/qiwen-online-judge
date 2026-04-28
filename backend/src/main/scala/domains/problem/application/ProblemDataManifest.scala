package domains.problem.application

import domains.problem.model.{ProblemDataPath, ProblemSlug}

final case class ProblemDataManifestEntry(
  path: ProblemDataPath,
  sizeBytes: Long,
  sha256: String
)

final case class ProblemDataManifest(
  problemSlug: ProblemSlug,
  entries: List[ProblemDataManifestEntry],
  version: String
)
