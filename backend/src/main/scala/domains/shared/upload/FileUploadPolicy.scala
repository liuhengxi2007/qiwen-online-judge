package domains.shared.upload

enum TextLineEndingPolicy:
  case Preserve
  case NormalizeLfForExtensions(extensions: Set[String])

final case class FileUploadPolicy(
  lineEndingPolicy: TextLineEndingPolicy = TextLineEndingPolicy.Preserve
)

object FileUploadPolicy:
  val preserve: FileUploadPolicy = FileUploadPolicy()
