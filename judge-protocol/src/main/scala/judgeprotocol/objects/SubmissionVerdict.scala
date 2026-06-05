package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

enum SubmissionVerdict:
  case Accepted
  case AcceptedByProtocol
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case SystemError

object SubmissionVerdict:
  def render(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.AcceptedByProtocol => "accepted_by_protocol"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap {
    case "accepted" => Right(SubmissionVerdict.Accepted)
    case "accepted_by_protocol" => Right(SubmissionVerdict.AcceptedByProtocol)
    case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
    case "compile_error" => Right(SubmissionVerdict.CompileError)
    case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
    case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
    case "system_error" => Right(SubmissionVerdict.SystemError)
    case other => Left(s"Unsupported submission verdict: $other")
  }
