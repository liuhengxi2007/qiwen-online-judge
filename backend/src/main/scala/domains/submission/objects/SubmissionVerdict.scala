package domains.submission.objects



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
