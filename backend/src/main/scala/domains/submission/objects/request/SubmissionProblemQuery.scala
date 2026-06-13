package domains.submission.objects.request

import io.circe.{Decoder, Encoder}


/** 提交列表中的题目搜索词。 */
final case class SubmissionProblemQuery(value: String)

/** 题目搜索词 JSON 编解码与非空校验入口。 */
object SubmissionProblemQuery:
  given Encoder[SubmissionProblemQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionProblemQuery] = Decoder.decodeString.emap(parse)

  /** 规范化题目搜索词；空白输入视为错误。 */
  def parse(raw: String): Either[String, SubmissionProblemQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Submission problem query is required.")
    else Right(SubmissionProblemQuery(normalized))
