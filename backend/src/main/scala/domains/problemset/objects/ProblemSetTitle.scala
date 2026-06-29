package domains.problemset.objects

import io.circe.{Decoder, Encoder}


/** 题单标题领域值，保存裁剪后的展示标题。 */
final case class ProblemSetTitle(value: String)

/** 提供题单标题 JSON codec 和非空/长度校验。 */
object ProblemSetTitle:
  given Encoder[ProblemSetTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetTitle] = Decoder.decodeString.emap(parse)

  /** 去除首尾空白，要求标题非空且最多 120 字符。 */
  def parse(raw: String): Either[String, ProblemSetTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set title is required.")
    else if normalized.length > 120 then Left("Problem set title must be at most 120 characters.")
    else Right(ProblemSetTitle(normalized))
