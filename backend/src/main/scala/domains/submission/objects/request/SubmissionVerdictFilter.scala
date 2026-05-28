package domains.submission.objects.request

import io.circe.{Decoder, Encoder}



enum SubmissionVerdictFilter:
  case All
  case Pending
  case Accepted
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case SystemError

object SubmissionVerdictFilter:
  given Encoder[SubmissionVerdictFilter] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionVerdictFilter] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, SubmissionVerdictFilter] =
    value.trim match
      case "all" => Right(SubmissionVerdictFilter.All)
      case "pending" => Right(SubmissionVerdictFilter.Pending)
      case "accepted" => Right(SubmissionVerdictFilter.Accepted)
      case "wrong_answer" => Right(SubmissionVerdictFilter.WrongAnswer)
      case "compile_error" => Right(SubmissionVerdictFilter.CompileError)
      case "runtime_error" => Right(SubmissionVerdictFilter.RuntimeError)
      case "time_limit_exceeded" => Right(SubmissionVerdictFilter.TimeLimitExceeded)
      case "system_error" => Right(SubmissionVerdictFilter.SystemError)
      case _ =>
        Left(
          "Submission verdict filter must be one of: all, pending, accepted, wrong_answer, compile_error, runtime_error, time_limit_exceeded, system_error."
        )

  def encode(value: SubmissionVerdictFilter): String =
    value match
      case SubmissionVerdictFilter.All => "all"
      case SubmissionVerdictFilter.Pending => "pending"
      case SubmissionVerdictFilter.Accepted => "accepted"
      case SubmissionVerdictFilter.WrongAnswer => "wrong_answer"
      case SubmissionVerdictFilter.CompileError => "compile_error"
      case SubmissionVerdictFilter.RuntimeError => "runtime_error"
      case SubmissionVerdictFilter.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdictFilter.SystemError => "system_error"
