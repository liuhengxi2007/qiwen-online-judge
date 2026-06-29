package domains.message.objects

import io.circe.{Decoder, Encoder}


/** 私信正文领域值，保存裁剪后的消息内容。 */
final case class MessageContent(value: String)

/** 提供私信正文 JSON codec 和非空/长度校验。 */
object MessageContent:
  given Encoder[MessageContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[MessageContent] = Decoder.decodeString.emap(parse)

  private val maxLength = 5000

  /** 去除首尾空白，要求消息非空且不超过 maxLength。 */
  def parse(raw: String): Either[String, MessageContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Message content is required.")
    else if normalized.length > maxLength then Left(s"Message content must be at most $maxLength characters.")
    else Right(MessageContent(normalized))
