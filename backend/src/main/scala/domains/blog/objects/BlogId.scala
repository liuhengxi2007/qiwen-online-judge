package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客公开 id 领域值，用于路由和跨表展示。 */
final case class BlogId(value: Long)

/** 提供博客公开 id codec 和路径解析。 */
object BlogId:
  given Encoder[BlogId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[BlogId] = Decoder.decodeLong.emap { value =>
    if value > 0 then Right(BlogId(value)) else Left("Blog id must be a positive integer.")
  }

  /** 将路径字符串解析为正整数博客 id。 */
  def parse(raw: String): Either[String, BlogId] =
    raw.toLongOption match
      case Some(value) if value > 0 => Right(BlogId(value))
      case _ => Left("Blog id must be a positive integer.")
