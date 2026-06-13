package shared.application.upload

import io.circe.{Decoder, Encoder}


/** 存储系统中的相对文件路径，禁止绝对路径、空段和目录穿越片段。 */
final case class StoredFilePath(value: String):
  /** 返回路径最后一段文件名，路径已由 parse 保证不含空段。 */
  def fileName: String =
    value.split('/').lastOption.getOrElse(value)

  /** 将子路径解析到当前目录下，并重新通过 parse 做边界校验。 */
  def resolve(child: StoredFilePath): Either[String, StoredFilePath] =
    StoredFilePath.parse(s"$value/${child.value}")

/** 提供存储路径的编解码和安全解析。 */
object StoredFilePath:
  given Encoder[StoredFilePath] = Encoder.encodeString.contramap(_.value)
  given Decoder[StoredFilePath] = Decoder.decodeString.emap(parse)

  private val separator = "/"

  /** 解析外部路径输入，统一斜杠并拒绝绝对路径、空段、. 或 .. 段。 */
  def parse(raw: String): Either[String, StoredFilePath] =
    val normalized = raw.trim.replace('\\', '/')
    if normalized.isEmpty then Left("Stored file path is required.")
    else if normalized.length > 1024 then Left("Stored file path must be at most 1024 characters.")
    else if normalized.startsWith(separator) || normalized.endsWith(separator) then
      Left("Stored file path must be relative and must not start or end with '/'.")
    else
      val segments = normalized.split(separator).toList
      if segments.exists(_.isEmpty) then Left("Stored file path must not contain empty segments.")
      else if segments.exists(segment => segment == "." || segment == "..") then
        Left("Stored file path must not contain '.' or '..' segments.")
      else if segments.exists(_.length > 255) then
        Left("Each stored file path segment must be at most 255 characters.")
      else Right(StoredFilePath(segments.mkString(separator)))
