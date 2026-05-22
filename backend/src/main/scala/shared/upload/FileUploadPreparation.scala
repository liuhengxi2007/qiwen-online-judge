package shared.upload



import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.util.zip.ZipInputStream
import scala.collection.mutable.ListBuffer

final case class PreparedUploadFile(
  path: StoredFilePath,
  bytes: Array[Byte]
)

object FileUploadPreparation:

  def prepareFile(
    path: StoredFilePath,
    bytes: Array[Byte],
    policy: FileUploadPolicy
  ): Either[String, PreparedUploadFile] =
    Right(PreparedUploadFile(path, normalizeTextLineEndings(path, bytes, policy)))

  def prepareArchive(
    archiveBytes: Array[Byte],
    targetDirectory: Option[StoredFilePath],
    policy: FileUploadPolicy
  ): Either[String, List[PreparedUploadFile]] =
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
