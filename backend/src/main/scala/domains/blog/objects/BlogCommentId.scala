package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客评论公开 id 领域值，用于路由和前端锚点。 */
final case class BlogCommentId(value: Long)

/** 提供评论公开 id codec 和路径解析。 */
object BlogCommentId:
  given Encoder[BlogCommentId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[BlogCommentId] = Decoder.decodeLong.emap { value =>
    if value > 0 then Right(BlogCommentId(value)) else Left("Blog comment id must be a positive integer.")
  }

  /** 将路径字符串解析为正整数评论 id。 */
  def parse(raw: String): Either[String, BlogCommentId] =
    raw.toLongOption match
      case Some(value) if value > 0 => Right(BlogCommentId(value))
      case _ => Left("Blog comment id must be a positive integer.")
