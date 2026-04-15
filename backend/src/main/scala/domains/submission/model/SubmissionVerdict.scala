package domains.submission.model

import io.circe.{Decoder, Encoder}

enum SubmissionVerdict:
  case Accepted
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case SystemError

object SubmissionVerdict:
  def parse(value: String): Either[String, SubmissionVerdict] =
    value.trim match
      case "accepted" => Right(SubmissionVerdict.Accepted)
      case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
      case "compile_error" => Right(SubmissionVerdict.CompileError)
      case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
      case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
      case "system_error" => Right(SubmissionVerdict.SystemError)
      case _ =>
        Left(
          "Submission verdict must be one of: accepted, wrong_answer, compile_error, runtime_error, time_limit_exceeded, system_error."
        )

  def fromDatabase(value: String): Option[SubmissionVerdict] =
    value match
      case "accepted" => Some(SubmissionVerdict.Accepted)
      case "wrong_answer" => Some(SubmissionVerdict.WrongAnswer)
      case "compile_error" => Some(SubmissionVerdict.CompileError)
      case "runtime_error" => Some(SubmissionVerdict.RuntimeError)
      case "time_limit_exceeded" => Some(SubmissionVerdict.TimeLimitExceeded)
      case "system_error" => Some(SubmissionVerdict.SystemError)
      case _ => None

  def toDatabase(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap(parse)
