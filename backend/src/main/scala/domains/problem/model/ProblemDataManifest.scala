package domains.problem.model

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

object ProblemDataManifest:
  def fromEntries(problemSlug: ProblemSlug, entries: List[ProblemDataManifestEntry]): ProblemDataManifest =
    val sortedEntries = entries.sortBy(_.path.value)
    ProblemDataManifest(problemSlug = problemSlug, entries = sortedEntries, version = manifestVersion(sortedEntries))

  private def manifestVersion(entries: List[ProblemDataManifestEntry]): String =
    sha256Hex(
      entries
        .map(entry => s"${entry.path.value}:${entry.sizeBytes}:${entry.sha256}")
        .mkString("\n")
        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
    )

  private def sha256Hex(bytes: Array[Byte]): String =
    java.security.MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
