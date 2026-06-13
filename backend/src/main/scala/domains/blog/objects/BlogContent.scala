package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客正文领域值，保存裁剪后的 Markdown/文本内容。 */
final case class BlogContent(value: String)

/** 提供博客正文 JSON codec 和非空/长度校验。 */
object BlogContent:
  given Encoder[BlogContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogContent] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白，要求正文非空且最多 200000 字符。 */
  def parse(raw: String): Either[String, BlogContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Blog content is required.")
    else if normalized.length > 200000 then Left("Blog content must be at most 200000 characters.")
    else Right(BlogContent(normalized))
