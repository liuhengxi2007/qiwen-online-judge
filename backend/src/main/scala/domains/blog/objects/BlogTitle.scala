package domains.blog.objects

import io.circe.{Decoder, Encoder}


/** 博客标题领域值，保存裁剪后的展示标题。 */
final case class BlogTitle(value: String)

/** 提供博客标题 JSON codec 和非空/长度校验。 */
object BlogTitle:
  given Encoder[BlogTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[BlogTitle] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白，要求标题非空且最多 160 字符。 */
  def parse(raw: String): Either[String, BlogTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Blog title is required.")
    else if normalized.length > 160 then Left("Blog title must be at most 160 characters.")
    else Right(BlogTitle(normalized))
