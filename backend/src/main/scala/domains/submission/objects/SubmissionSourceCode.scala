package domains.submission.objects

import io.circe.{Decoder, Encoder}


/** 提交源码文本；保留原始换行和空白，只限制非空与字符数。 */
final case class SubmissionSourceCode(value: String)

/** 提交源码 JSON 编解码与大小校验入口。 */
object SubmissionSourceCode:
  val MaxChars: Int = 200000

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(parse)

  /** 校验源码非空且不超过最大字符数；不修改源码内容。 */
  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Source code is required.")
    else if raw.length > MaxChars then Left(s"Source code must be at most $MaxChars characters.")
    else Right(SubmissionSourceCode(raw))
