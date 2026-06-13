package shared.application.upload



/** 文本换行处理策略，用于上传文件入库前的可选规范化。 */
enum TextLineEndingPolicy:
  case Preserve
  case NormalizeLfForExtensions(extensions: Set[String])

/** 文件上传预处理策略，目前仅控制文本文件换行行为。 */
final case class FileUploadPolicy(
  lineEndingPolicy: TextLineEndingPolicy = TextLineEndingPolicy.Preserve
)

/** 文件上传策略预设。 */
object FileUploadPolicy:
  val preserve: FileUploadPolicy = FileUploadPolicy()
