package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 表示 judger 协议支持的提交语言或静态输出类型。 */
enum SubmissionLanguage:
  case Cpp17
  case Python3
  case Text

/** 提供提交语言与 wire-format 字符串之间的稳定互转。 */
object SubmissionLanguage:
  /** 将语言枚举渲染为任务构建器、worker 和结果页共用的协议值。 */
  def render(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"
      case SubmissionLanguage.Text => "text"

  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap {
    case "cpp17" => Right(SubmissionLanguage.Cpp17)
    case "python3" => Right(SubmissionLanguage.Python3)
    case "text" => Right(SubmissionLanguage.Text)
    case other => Left(s"Unsupported submission language: $other")
  }
