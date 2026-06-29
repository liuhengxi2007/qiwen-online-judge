package domains.problem.objects.internal

import domains.problem.objects.ProblemSlug

/** 题目数据清单，由 ProblemDataStorage/ProblemDataFileTable 生成，并经 GetJudgeProblemDataManifest 供 JudgeTaskBuilder 使用。 */
final case class ProblemDataManifest(
  problemSlug: ProblemSlug,
  entries: List[ProblemDataManifestEntry],
  version: String
)

/** 题目数据清单的构造工具；负责排序条目并生成稳定版本号。 */
object ProblemDataManifest:
  /** 从文件条目生成清单；输出版本随路径、大小或 sha256 变化。 */
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
