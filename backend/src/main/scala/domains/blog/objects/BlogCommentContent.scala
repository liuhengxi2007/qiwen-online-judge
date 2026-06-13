package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客评论内容领域值，保存裁剪后的正文。 */
final case class BlogCommentContent(value: String)

/** 提供评论内容 JSON codec 和非空/长度校验。 */
object BlogCommentContent:
  given Encoder[BlogCommentContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogCommentContent] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白，要求评论非空且最多 20000 字符。 */
  def parse(raw: String): Either[String, BlogCommentContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Comment content is required.")
    else if normalized.length > 20000 then Left("Comment content must be at most 20000 characters.")
    else Right(BlogCommentContent(normalized))
