package domains.submission.objects.request

import io.circe.{Decoder, Encoder}


/** 提交列表中的用户搜索词，可匹配用户名或展示名。 */
final case class SubmissionUserQuery(value: String)

/** 用户搜索词 JSON 编解码与非空校验入口。 */
object SubmissionUserQuery:
  given Encoder[SubmissionUserQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionUserQuery] = Decoder.decodeString.emap(parse)

  /** 规范化用户搜索词；空白输入视为错误。 */
  def parse(raw: String): Either[String, SubmissionUserQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Submission username query is required.")
    else Right(SubmissionUserQuery(normalized))
