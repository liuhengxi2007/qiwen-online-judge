package shared.application.upload



/** 文本换行处理策略，用于上传文件入库前的可选规范化。 */
enum TextLineEndingPolicy:
  case Preserve
  case NormalizeLfForExtensions(extensions: Set[String])

/** 文件上传预处理策略，控制文本换行和 zip 解包资源边界。 */
final case class FileUploadPolicy(
  lineEndingPolicy: TextLineEndingPolicy = TextLineEndingPolicy.Preserve,
  maxArchiveFileCount: Int = 1000,
  maxArchiveEntryBytes: Long = 256L * 1024L * 1024L,
  maxArchiveTotalBytes: Long = 512L * 1024L * 1024L
)

/** 文件上传策略预设。 */
object FileUploadPolicy:
  val preserve: FileUploadPolicy = FileUploadPolicy()
