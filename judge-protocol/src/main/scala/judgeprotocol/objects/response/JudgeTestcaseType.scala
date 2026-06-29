package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}

/** 测试点类别；main 参与基础分，sample/hack 主要用于展示或最差结果。 */
enum JudgeTestcaseType:
  case Main
  case Sample
  case Hack

/** 提供测试点类别的协议编解码和字符串转换。 */
object JudgeTestcaseType:
  given Encoder[JudgeTestcaseType] = Encoder.encodeString.contramap(render)
  given Decoder[JudgeTestcaseType] = Decoder.decodeString.emap(parse)

  /** 将测试点类别渲染成 judge.yaml/backend/judger 共享的协议值。 */
  def render(value: JudgeTestcaseType): String =
    value match
      case Main => "main"
      case Sample => "sample"
      case Hack => "hack"

  /** 从协议字符串解析测试点类别；未知值会导致 JSON 解码失败。 */
  def parse(raw: String): Either[String, JudgeTestcaseType] =
    raw.trim match
      case "main" => Right(Main)
      case "sample" => Right(Sample)
      case "hack" => Right(Hack)
      case other => Left(s"Unsupported testcase type: $other.")
