package domains.blog.objects

import io.circe.{Decoder, Encoder}


final case class BlogCommentContent(value: String)

object BlogCommentContent:
  given Encoder[BlogCommentContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogCommentContent] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, BlogCommentContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Comment content is required.")
    else if normalized.length > 20000 then Left("Comment content must be at most 20000 characters.")
    else Right(BlogCommentContent(normalized))
