package shared.upload



import io.circe.{Decoder, Encoder}

final case class StoredFilePath(value: String):
  def fileName: String =
    value.split('/').lastOption.getOrElse(value)

  def resolve(child: StoredFilePath): Either[String, StoredFilePath] =
    StoredFilePath.parse(s"$value/${child.value}")

object StoredFilePath:
  private val separator = "/"

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

  given Encoder[StoredFilePath] = Encoder.encodeString.contramap(_.value)
  given Decoder[StoredFilePath] = Decoder.decodeString.emap(parse)
