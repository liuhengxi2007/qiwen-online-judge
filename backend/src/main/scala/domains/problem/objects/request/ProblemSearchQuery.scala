package domains.problem.objects.request

import io.circe.{Decoder, Encoder}


/** 题目搜索关键词；用于列表过滤和建议匹配。 */
final case class ProblemSearchQuery(value: String)

/** 题目搜索关键词的 JSON 编解码与非空校验入口。 */
object ProblemSearchQuery:
  given Encoder[ProblemSearchQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSearchQuery] = Decoder.decodeString.emap(parse)

  /** 规范化搜索词；空白搜索词在需要明确搜索的接口中视为错误。 */
  def parse(raw: String): Either[String, ProblemSearchQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem search query is required.")
    else Right(ProblemSearchQuery(normalized))
