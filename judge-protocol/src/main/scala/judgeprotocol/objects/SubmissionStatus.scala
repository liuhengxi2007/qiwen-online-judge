package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 描述提交在 backend 持久化和 judger 回报链路中的生命周期状态。 */
enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

/** 提供提交状态与协议 JSON 字符串之间的稳定映射。 */
object SubmissionStatus:
  /** 将内部 ADT 渲染为 backend/frontend 约定的协议值；不产生副作用。 */
  def render(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap {
    case "queued" => Right(SubmissionStatus.Queued)
    case "running" => Right(SubmissionStatus.Running)
    case "completed" => Right(SubmissionStatus.Completed)
    case "failed" => Right(SubmissionStatus.Failed)
    case other => Left(s"Unsupported submission status: $other")
  }
