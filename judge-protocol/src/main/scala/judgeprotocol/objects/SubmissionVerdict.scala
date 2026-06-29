package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

/** 表示单个测试点、子任务或整题聚合后的判题结论。 */
enum SubmissionVerdict:
  case Accepted
  case AcceptedByProtocol
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case IdlenessLimitExceeded
  case SystemError

/** 提供判题结论与 JSON 协议字符串的互转。 */
object SubmissionVerdict:
  /** 将判题结论渲染为 backend 存储和前端展示使用的稳定字符串。 */
  def render(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.AcceptedByProtocol => "accepted_by_protocol"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.IdlenessLimitExceeded => "idleness_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap {
    case "accepted" => Right(SubmissionVerdict.Accepted)
    case "accepted_by_protocol" => Right(SubmissionVerdict.AcceptedByProtocol)
    case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
    case "compile_error" => Right(SubmissionVerdict.CompileError)
    case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
    case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
    case "idleness_limit_exceeded" => Right(SubmissionVerdict.IdlenessLimitExceeded)
    case "system_error" => Right(SubmissionVerdict.SystemError)
    case other => Left(s"Unsupported submission verdict: $other")
  }
