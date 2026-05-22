package domains.blog.model



import io.circe.{Decoder, Encoder}

final case class BlogId(value: Long)

object BlogId:
  def parse(raw: String): Either[String, BlogId] =
    raw.toLongOption match
      case Some(value) if value > 0 => Right(BlogId(value))
      case _ => Left("Blog id must be a positive integer.")

  given Encoder[BlogId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[BlogId] = Decoder.decodeLong.emap(value => if value > 0 then Right(BlogId(value)) else Left("Blog id must be a positive integer."))
