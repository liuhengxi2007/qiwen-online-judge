package domains.problem.objects

final case class ProblemDataManifestEntry(
  path: ProblemDataPath,
  sizeBytes: Long,
  sha256: String
)
