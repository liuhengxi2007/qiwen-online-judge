package domains.submission.objects

import io.circe.{Decoder, Encoder}

/** 提交结果展示模式；题目 ready 校验时根据判题任务聚合方式确定。 */
enum SubmissionResultDisplayMode:
  case Verdict
  case Score

/** 提交结果展示模式的 JSON/数据库字符串编解码器。 */
object SubmissionResultDisplayMode:
  given Encoder[SubmissionResultDisplayMode] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionResultDisplayMode] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为展示模式。 */
  def parse(value: String): Either[String, SubmissionResultDisplayMode] =
    value.trim match
      case "verdict" => Right(SubmissionResultDisplayMode.Verdict)
      case "score" => Right(SubmissionResultDisplayMode.Score)
      case _ => Left("Submission result display mode must be one of: verdict, score.")

  /** 将展示模式编码为数据库和 JSON 字符串。 */
  def encode(value: SubmissionResultDisplayMode): String =
    value match
      case SubmissionResultDisplayMode.Verdict => "verdict"
      case SubmissionResultDisplayMode.Score => "score"
