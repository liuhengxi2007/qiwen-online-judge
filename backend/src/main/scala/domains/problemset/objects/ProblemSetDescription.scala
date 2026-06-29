package domains.problemset.objects

import io.circe.{Decoder, Encoder}


/** 题单描述领域值，保存裁剪后的说明文本。 */
final case class ProblemSetDescription(value: String)

/** 提供题单描述 JSON codec 和长度校验。 */
object ProblemSetDescription:
  given Encoder[ProblemSetDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetDescription] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白并限制描述最多 2000 字符。 */
  def parse(raw: String): Either[String, ProblemSetDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("Problem set description must be at most 2000 characters.")
    else Right(ProblemSetDescription(normalized))
