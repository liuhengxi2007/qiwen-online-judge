package domains.submission.objects

import io.circe.{Decoder, Encoder}


/** 后端支持的提交语言；Text 用于答案文件或纯文本角色。 */
enum SubmissionLanguage:
  case Cpp17
  case Python3
  case Text

/** 提交语言的 JSON/数据库字符串编解码器。 */
object SubmissionLanguage:
  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为提交语言。 */
  def parse(value: String): Either[String, SubmissionLanguage] =
    value.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case "python3" => Right(SubmissionLanguage.Python3)
      case "text" => Right(SubmissionLanguage.Text)
      case _ => Left("Submission language must be one of: cpp17, python3, text.")

  private def encode(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"
      case SubmissionLanguage.Text => "text"
