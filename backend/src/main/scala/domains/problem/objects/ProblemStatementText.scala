package domains.problem.objects

import io.circe.{Decoder, Encoder}


/** 题面正文文本；后端只校验非空和长度，不解释具体 Markdown/HTML 语义。 */
final case class ProblemStatementText(value: String)

/** 题面正文的 JSON 编解码与输入校验入口。 */
object ProblemStatementText:
  given Encoder[ProblemStatementText] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemStatementText] = Decoder.decodeString.emap(parse)

  /** 规范化并校验题面正文；空文本和超长文本会被拒绝。 */
  def parse(raw: String): Either[String, ProblemStatementText] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem statement is required.")
    else if normalized.length > 20000 then Left("Problem statement must be at most 20000 characters.")
    else Right(ProblemStatementText(normalized))
