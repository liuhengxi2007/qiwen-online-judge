package domains.hack.objects

import io.circe.{Decoder, Encoder}

/** hack attempt 生命周期和结果状态；queued/running 为非终态，其余为 worker 上报终态。 */
enum HackStatus:
  case Queued
  case Running
  case Success
  case NoEffect
  case Invalid
  case Failed

/** hack 状态的 JSON/数据库字符串编解码器。 */
object HackStatus:
  given Encoder[HackStatus] = Encoder.encodeString.contramap(encode)
  given Decoder[HackStatus] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为 hack 状态。 */
  def parse(value: String): Either[String, HackStatus] =
    value.trim match
      case "queued" => Right(HackStatus.Queued)
      case "running" => Right(HackStatus.Running)
      case "success" => Right(HackStatus.Success)
      case "no_effect" => Right(HackStatus.NoEffect)
      case "invalid" => Right(HackStatus.Invalid)
      case "failed" => Right(HackStatus.Failed)
      case _ => Left("Hack status must be one of: queued, running, success, no_effect, invalid, failed.")

  /** 将 hack 状态编码为数据库和 JSON 字符串。 */
  def encode(value: HackStatus): String =
    value match
      case HackStatus.Queued => "queued"
      case HackStatus.Running => "running"
      case HackStatus.Success => "success"
      case HackStatus.NoEffect => "no_effect"
      case HackStatus.Invalid => "invalid"
      case HackStatus.Failed => "failed"

  /** 判断状态是否可作为 worker 完成回报写入 finished_at。 */
  def isTerminal(value: HackStatus): Boolean =
    value match
      case HackStatus.Success | HackStatus.NoEffect | HackStatus.Invalid | HackStatus.Failed => true
      case HackStatus.Queued | HackStatus.Running => false
