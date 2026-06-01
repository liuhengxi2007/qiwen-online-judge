package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskFilePath private (value: String) extends AnyVal

object JudgeTaskFilePath:
  given Encoder[JudgeTaskFilePath] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgeTaskFilePath] = Decoder.decodeString.emap(parse)

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

final case class JudgeTaskFileSizeBytes private (value: Long) extends AnyVal

object JudgeTaskFileSizeBytes:
  given Encoder[JudgeTaskFileSizeBytes] = Encoder.encodeLong.contramap(_.value)
  given Decoder[JudgeTaskFileSizeBytes] = Decoder.decodeLong.emap(parse)

  def parse(value: Long): Either[String, JudgeTaskFileSizeBytes] =
    Either.cond(value >= 0L, JudgeTaskFileSizeBytes(value), "file size must be non-negative")

final case class JudgeTaskFileSha256 private (value: String) extends AnyVal

object JudgeTaskFileSha256:
  private val pattern = "^[a-fA-F0-9]{64}$".r

  given Encoder[JudgeTaskFileSha256] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgeTaskFileSha256] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, JudgeTaskFileSha256] =
    raw.trim match
      case pattern() => Right(JudgeTaskFileSha256(raw.trim.toLowerCase))
      case _ => Left("sha256 must be 64 hexadecimal characters")

final case class JudgeTaskFileRef(
  path: JudgeTaskFilePath,
  sizeBytes: JudgeTaskFileSizeBytes,
  sha256: JudgeTaskFileSha256
)

object JudgeTaskFileRef:
  def from(path: String, sizeBytes: Long, sha256: String): Either[String, JudgeTaskFileRef] =
    for
      parsedPath <- JudgeTaskFilePath.parse(path)
      parsedSize <- JudgeTaskFileSizeBytes.parse(sizeBytes)
      parsedSha256 <- JudgeTaskFileSha256.parse(sha256)
    yield JudgeTaskFileRef(parsedPath, parsedSize, parsedSha256)

  def unsafe(path: String, sizeBytes: Long, sha256: String): JudgeTaskFileRef =
    from(path, sizeBytes, sha256).fold(message => throw IllegalArgumentException(message), identity)

  given Encoder[JudgeTaskFileRef] = deriveEncoder[JudgeTaskFileRef]
  given Decoder[JudgeTaskFileRef] = deriveDecoder[JudgeTaskFileRef]
