package domains.submission.http.codec

import domains.submission.model.*
import io.circe.{Decoder, Encoder}

object SubmissionModelHttpCodecs:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(encodeSubmissionLanguage)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap(SubmissionLanguage.parse)

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(SubmissionSourceCode.parse)

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(encodeSubmissionVerdict)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap(SubmissionVerdict.parse)

  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(encodeSubmissionStatus)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap(SubmissionStatus.parse)

  private def encodeSubmissionLanguage(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"

  private def encodeSubmissionVerdict(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  private def encodeSubmissionStatus(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"
