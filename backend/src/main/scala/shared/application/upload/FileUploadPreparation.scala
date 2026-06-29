package shared.application.upload



import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.util.zip.ZipInputStream
import scala.annotation.tailrec

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
    val zipInputStream = ZipInputStream(ByteArrayInputStream(archiveBytes))

    @tailrec
    def readEntries(preparedFiles: List[PreparedUploadFile], totalBytes: Long): Either[String, List[PreparedUploadFile]] =
      val entry = zipInputStream.getNextEntry
      if entry == null then
        Right(preparedFiles.reverse)
      else
        val entryResult: Either[String, (List[PreparedUploadFile], Long)] =
          try
            if entry.isDirectory then
              Right((preparedFiles, totalBytes))
            else if preparedFiles.size >= policy.maxArchiveFileCount then
              Left(s"Uploaded archive must contain at most ${policy.maxArchiveFileCount} files.")
            else
              readEntryBytes(zipInputStream, policy.maxArchiveEntryBytes, policy.maxArchiveTotalBytes - totalBytes) match
                case Left(message) =>
                  Left(message)
                case Right(entryBytes) =>
                  val nextTotalBytes = totalBytes + entryBytes.length.toLong
                  val prepared = for
                    entryPath <- StoredFilePath.parse(entry.getName)
                    resolvedPath <- targetDirectory match
                      case Some(directory) => directory.resolve(entryPath)
                      case None => Right(entryPath)
                    file <- prepareFile(resolvedPath, entryBytes, policy)
                  yield file
                  prepared.map(file => (file :: preparedFiles, nextTotalBytes))
          finally zipInputStream.closeEntry()
        entryResult match
          case Left(message) => Left(message)
          case Right((nextPreparedFiles, nextTotalBytes)) => readEntries(nextPreparedFiles, nextTotalBytes)

    try
      readEntries(Nil, 0L)
    catch
      case _: IOException => Left("Uploaded archive is not a valid zip file.")
    finally zipInputStream.close()

  private def readEntryBytes(
    zipInputStream: ZipInputStream,
    maxEntryBytes: Long,
    remainingTotalBytes: Long
  ): Either[String, Array[Byte]] =
    val output = ByteArrayOutputStream()
    val buffer = Array.ofDim[Byte](8192)

    @tailrec
    def readNext(entryBytes: Long): Either[String, Array[Byte]] =
      val read = zipInputStream.read(buffer)
      if read == -1 then
        Right(output.toByteArray)
      else
        val nextEntryBytes = entryBytes + read.toLong
        if nextEntryBytes > maxEntryBytes then
          Left(s"Each uploaded archive file must be at most ${maxEntryBytes / 1024L / 1024L} MB.")
        else if nextEntryBytes > remainingTotalBytes then
          Left("Uploaded archive extracted content is too large.")
        else
          output.write(buffer, 0, read)
          readNext(nextEntryBytes)

    readNext(0L)

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
