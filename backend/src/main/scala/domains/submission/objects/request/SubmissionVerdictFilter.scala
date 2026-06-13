package domains.submission.objects.request

import io.circe.{Decoder, Encoder}



/** 提交列表结论过滤器；All/Pending 是列表层额外语义，其余映射到提交结论。 */
enum SubmissionVerdictFilter:
  case All
  case Pending
  case Accepted
  case AcceptedByProtocol
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case IdlenessLimitExceeded
  case SystemError

/** 提交结论过滤器的 JSON/query 字符串编解码器。 */
object SubmissionVerdictFilter:
  given Encoder[SubmissionVerdictFilter] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionVerdictFilter] = Decoder.decodeString.emap(parse)

  /** 将外部字符串解析为结论过滤器。 */
  def parse(value: String): Either[String, SubmissionVerdictFilter] =
    value.trim match
      case "all" => Right(SubmissionVerdictFilter.All)
      case "pending" => Right(SubmissionVerdictFilter.Pending)
      case "accepted" => Right(SubmissionVerdictFilter.Accepted)
      case "accepted_by_protocol" => Right(SubmissionVerdictFilter.AcceptedByProtocol)
      case "wrong_answer" => Right(SubmissionVerdictFilter.WrongAnswer)
      case "compile_error" => Right(SubmissionVerdictFilter.CompileError)
      case "runtime_error" => Right(SubmissionVerdictFilter.RuntimeError)
      case "time_limit_exceeded" => Right(SubmissionVerdictFilter.TimeLimitExceeded)
      case "idleness_limit_exceeded" => Right(SubmissionVerdictFilter.IdlenessLimitExceeded)
      case "system_error" => Right(SubmissionVerdictFilter.SystemError)
      case _ =>
        Left(
          "Submission verdict filter must be one of: all, pending, accepted, accepted_by_protocol, wrong_answer, compile_error, runtime_error, time_limit_exceeded, idleness_limit_exceeded, system_error."
        )

  /** 将结论过滤器编码为字符串。 */
  def encode(value: SubmissionVerdictFilter): String =
    value match
      case SubmissionVerdictFilter.All => "all"
      case SubmissionVerdictFilter.Pending => "pending"
      case SubmissionVerdictFilter.Accepted => "accepted"
      case SubmissionVerdictFilter.AcceptedByProtocol => "accepted_by_protocol"
      case SubmissionVerdictFilter.WrongAnswer => "wrong_answer"
      case SubmissionVerdictFilter.CompileError => "compile_error"
      case SubmissionVerdictFilter.RuntimeError => "runtime_error"
      case SubmissionVerdictFilter.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdictFilter.IdlenessLimitExceeded => "idleness_limit_exceeded"
      case SubmissionVerdictFilter.SystemError => "system_error"
