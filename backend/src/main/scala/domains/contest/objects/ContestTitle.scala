package domains.contest.objects

import io.circe.{Decoder, Encoder}

/** 比赛标题领域值，保存裁剪后的展示标题。 */
final case class ContestTitle(value: String)

/** 提供比赛标题 JSON codec 和非空/长度校验。 */
object ContestTitle:
  given Encoder[ContestTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ContestTitle] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白，要求标题非空且最多 120 字符。 */
  def parse(raw: String): Either[String, ContestTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Contest title is required.")
    else if normalized.length > 120 then Left("Contest title must be at most 120 characters.")
    else Right(ContestTitle(normalized))
