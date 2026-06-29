package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题目标题；用于列表、详情和提交来源展示。 */
final case class ProblemTitle(value: String)

/** 题目标题的 JSON 编解码与输入校验入口。 */
object ProblemTitle:
  given Encoder[ProblemTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemTitle] = Decoder.decodeString.emap(parse)

  /** 规范化并校验标题；空标题和超长标题会被拒绝。 */
  def parse(raw: String): Either[String, ProblemTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem title is required.")
    else if normalized.length > 120 then Left("Problem title must be at most 120 characters.")
    else Right(ProblemTitle(normalized))
