package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 题目数据文件在 problem data 包内的相对路径，不允许绝对路径或目录穿越。 */
final case class JudgeTaskFilePath private (value: String) extends AnyVal

/** 负责题目数据文件相对路径的协议校验和编解码。 */
object JudgeTaskFilePath:
  given Encoder[JudgeTaskFilePath] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgeTaskFilePath] = Decoder.decodeString.emap(parse)

  /** 规范化并校验文件路径；输出可安全交给 judger 缓存和下载接口使用的相对路径。 */
  def parse(raw: String): Either[String, JudgeTaskFilePath] =
    val normalized = raw.trim.replace('\\', '/')
    if normalized.isEmpty then Left("file path is required")
    else if normalized.length > 1024 then Left("file path must be at most 1024 characters")
    else if normalized.startsWith("/") || normalized.endsWith("/") then Left("file path must be relative and must not end with /")
    else
      val segments = normalized.split('/').toList
      if segments.exists(_.isEmpty) then Left("file path must not contain empty segments")
      else if segments.exists(segment => segment == "." || segment == "..") then Left("file path must not contain . or .. segments")
      else if segments.exists(_.length > 255) then Left("file path segments must be at most 255 characters")
      else Right(JudgeTaskFilePath(normalized))

/** 题目数据文件大小，来自 backend manifest，用于 worker 侧记录和审计。 */
final case class JudgeTaskFileSizeBytes private (value: Long) extends AnyVal

/** 负责文件大小的协议校验，当前只要求非负。 */
object JudgeTaskFileSizeBytes:
  given Encoder[JudgeTaskFileSizeBytes] = Encoder.encodeLong.contramap(_.value)
  given Decoder[JudgeTaskFileSizeBytes] = Decoder.decodeLong.emap(parse)

  /** 从 manifest 数值构造文件大小；不会检查实际下载字节数。 */
  def parse(value: Long): Either[String, JudgeTaskFileSizeBytes] =
    Either.cond(value >= 0L, JudgeTaskFileSizeBytes(value), "file size must be non-negative")

/** 题目数据文件内容的 sha256 摘要，用作缓存文件名和完整性校验键。 */
final case class JudgeTaskFileSha256 private (value: String) extends AnyVal

/** 校验 sha256 摘要并统一为小写十六进制。 */
object JudgeTaskFileSha256:
  private val pattern = "^[a-fA-F0-9]{64}$".r

  given Encoder[JudgeTaskFileSha256] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgeTaskFileSha256] = Decoder.decodeString.emap(parse)

  /** 从外部字符串构造摘要；只接受 64 位十六进制，避免缓存路径被注入。 */
  def parse(raw: String): Either[String, JudgeTaskFileSha256] =
    raw.trim match
      case pattern() => Right(JudgeTaskFileSha256(raw.trim.toLowerCase))
      case _ => Left("sha256 must be 64 hexadecimal characters")

/** worker 下载题目数据所需的文件引用，绑定相对路径、大小和 sha256。 */
final case class JudgeTaskFileRef(
  path: JudgeTaskFilePath,
  sizeBytes: JudgeTaskFileSizeBytes,
  sha256: JudgeTaskFileSha256
)

/** 提供题目数据文件引用的安全构造和协议编解码。 */
object JudgeTaskFileRef:
  /** 同时校验路径、大小和摘要；失败信息用于任务构建阶段暴露配置错误。 */
  def from(path: String, sizeBytes: Long, sha256: String): Either[String, JudgeTaskFileRef] =
    for
      parsedPath <- JudgeTaskFilePath.parse(path)
      parsedSize <- JudgeTaskFileSizeBytes.parse(sizeBytes)
      parsedSha256 <- JudgeTaskFileSha256.parse(sha256)
    yield JudgeTaskFileRef(parsedPath, parsedSize, parsedSha256)

  given Encoder[JudgeTaskFileRef] = deriveEncoder[JudgeTaskFileRef]
  given Decoder[JudgeTaskFileRef] = deriveDecoder[JudgeTaskFileRef]
