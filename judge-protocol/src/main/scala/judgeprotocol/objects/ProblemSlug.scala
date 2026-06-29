package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 题目在 worker 协议中的公开 slug，用于定位题目数据下载接口。 */
final case class ProblemSlug(value: String)

/** 负责题目 slug 的协议编解码，并在边界处去除空白。 */
object ProblemSlug:
  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap { value =>
    val normalized = value.trim
    if normalized.isEmpty then Left("Problem slug is required.") else Right(ProblemSlug(normalized))
  }
