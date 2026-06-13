package domains.submission.objects

import io.circe.{Decoder, Encoder}


/** 提交判题生命周期状态；queued/running 为非终态，completed/failed 为终态。 */
enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

/** 提交状态的 JSON/数据库字符串编解码器。 */
object SubmissionStatus:
  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为提交状态。 */
  def parse(value: String): Either[String, SubmissionStatus] =
    value.trim match
      case "queued" => Right(SubmissionStatus.Queued)
      case "running" => Right(SubmissionStatus.Running)
      case "completed" => Right(SubmissionStatus.Completed)
      case "failed" => Right(SubmissionStatus.Failed)
      case _ => Left("Submission status must be one of: queued, running, completed, failed.")

  private def encode(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"
