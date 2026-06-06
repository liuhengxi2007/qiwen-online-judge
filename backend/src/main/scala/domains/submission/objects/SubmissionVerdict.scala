package domains.submission.objects

import io.circe.{Decoder, Encoder}


enum SubmissionVerdict:
  case Accepted
  case AcceptedByProtocol
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case IdlenessLimitExceeded
  case SystemError

object SubmissionVerdict:
  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, SubmissionVerdict] =
    value.trim match
      case "accepted" => Right(SubmissionVerdict.Accepted)
      case "accepted_by_protocol" => Right(SubmissionVerdict.AcceptedByProtocol)
      case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
      case "compile_error" => Right(SubmissionVerdict.CompileError)
      case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
      case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
      case "idleness_limit_exceeded" => Right(SubmissionVerdict.IdlenessLimitExceeded)
      case "system_error" => Right(SubmissionVerdict.SystemError)
      case _ =>
        Left(
          "Submission verdict must be one of: accepted, accepted_by_protocol, wrong_answer, compile_error, runtime_error, time_limit_exceeded, idleness_limit_exceeded, system_error."
        )

  private def encode(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.AcceptedByProtocol => "accepted_by_protocol"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.IdlenessLimitExceeded => "idleness_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"
