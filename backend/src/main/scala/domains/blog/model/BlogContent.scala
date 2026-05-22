package domains.blog.model



import io.circe.{Decoder, Encoder}

final case class BlogContent(value: String)

object BlogContent:
  def parse(raw: String): Either[String, BlogContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Blog content is required.")
    else if normalized.length > 200000 then Left("Blog content must be at most 200000 characters.")
    else Right(BlogContent(normalized))

  given Encoder[BlogContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogContent] = Decoder.decodeString.emap(parse)
