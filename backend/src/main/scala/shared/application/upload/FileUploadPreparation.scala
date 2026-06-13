package shared.application.upload



import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.util.zip.ZipInputStream
import scala.collection.mutable.ListBuffer

/** 已完成路径校验和内容预处理的上传文件。 */
final case class PreparedUploadFile(
  path: StoredFilePath,
  bytes: Array[Byte]
)

/** 文件上传预处理入口，负责普通文件和 zip 归档中的路径校验与换行规范化。 */
object FileUploadPreparation:

  /** 对单个文件应用上传策略，返回可持久化的文件路径和字节内容。 */
  def prepareFile(
    path: StoredFilePath,
    bytes: Array[Byte],
    policy: FileUploadPolicy
  ): Either[String, PreparedUploadFile] =
    Right(PreparedUploadFile(path, normalizeTextLineEndings(path, bytes, policy)))

  /** 解包 zip 归档并逐项校验路径；非法路径或预处理失败以 Left 返回。 */
  def prepareArchive(
    archiveBytes: Array[Byte],
    targetDirectory: Option[StoredFilePath],
    policy: FileUploadPolicy
  ): Either[String, List[PreparedUploadFile]] =
    /** FIXME-CN: zip 解包没有总大小、文件数量或单项大小限制，恶意归档可能导致内存或磁盘配额风险。 */
    val zipInputStream = ZipInputStream(ByteArrayInputStream(archiveBytes))
    val preparedFiles = ListBuffer.empty[PreparedUploadFile]
    try
      Iterator
        .continually(zipInputStream.getNextEntry)
        .takeWhile(_ != null)
        .foreach { entry =>
          if !entry.isDirectory then
            val rawEntryPath = StoredFilePath.parse(entry.getName)
            val resolvedPath = rawEntryPath.flatMap { entryPath =>
              targetDirectory match
                case Some(directory) => directory.resolve(entryPath)
                case None => Right(entryPath)
            }
            val entryBytes = zipInputStream.readAllBytes()
            val prepared = resolvedPath.flatMap(path => prepareFile(path, entryBytes, policy))
            prepared match
              /** FIXME-CN: 这里用 IllegalArgumentException 从 foreach 中短路业务校验，校验错误与真实运行时同类异常共用 catch 边界。 */
              case Left(message) => throw IllegalArgumentException(message)
              case Right(file) => preparedFiles += file
          zipInputStream.closeEntry()
        }
      Right(preparedFiles.toList)
    catch
      case error: IllegalArgumentException => Left(error.getMessage)
    finally zipInputStream.close()

  private def normalizeTextLineEndings(
    path: StoredFilePath,
    bytes: Array[Byte],
    policy: FileUploadPolicy
  ): Array[Byte] =
    policy.lineEndingPolicy match
      case TextLineEndingPolicy.Preserve =>
        bytes
      case TextLineEndingPolicy.NormalizeLfForExtensions(extensions) =>
        val fileName = path.fileName.toLowerCase
        val shouldNormalize = extensions.exists(extension => fileName.endsWith(extension.toLowerCase))
        if !shouldNormalize then bytes
        else
          decodeUtf8(bytes) match
            case Left(_) => bytes
            case Right(text) =>
              text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .getBytes(StandardCharsets.UTF_8)

  private def decodeUtf8(bytes: Array[Byte]): Either[CharacterCodingException, String] =
    val decoder = StandardCharsets.UTF_8
      .newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
    try Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    catch
      case error: CharacterCodingException => Left(error)
