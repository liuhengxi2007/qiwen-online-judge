package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 提交源代码或工具源代码的文本载体，保持原始内容不做格式化。 */
final case class SubmissionSourceCode(value: String)

/** 校验源代码协议输入，限制空内容和过大的 payload。 */
object SubmissionSourceCode:
  /** 从请求文本构造源代码；保留原始换行和空白，仅用 trim 判断是否为空。 */
  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Submission source code is required.")
    else if raw.length > 200000 then Left("Submission source code must be at most 200000 characters.")
    else Right(SubmissionSourceCode(raw))

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(parse)
