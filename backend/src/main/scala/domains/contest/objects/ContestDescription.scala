package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛描述领域值，保存裁剪后的说明文本。 */
final case class ContestDescription(value: String)

/** 提供比赛描述 JSON codec 和长度校验。 */
object ContestDescription:
  given Encoder[ContestDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestDescription] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白并限制描述最多 4000 字符。 */
  def parse(raw: String): Either[String, ContestDescription] =
    val normalized = raw.trim
    if normalized.length > 4000 then Left("Contest description must be at most 4000 characters.")
    else Right(ContestDescription(normalized))
