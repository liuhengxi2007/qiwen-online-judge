package domains.blog.objects

import io.circe.{Decoder, Encoder}


final case class BlogCommentId(value: Long)

object BlogCommentId:
  given Encoder[BlogCommentId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[BlogCommentId] = Decoder.decodeLong.emap { value =>
    if value > 0 then Right(BlogCommentId(value)) else Left("Blog comment id must be a positive integer.")
  }

  def parse(raw: String): Either[String, BlogCommentId] =
    raw.toLongOption match
      case Some(value) if value > 0 => Right(BlogCommentId(value))
      case _ => Left("Blog comment id must be a positive integer.")
