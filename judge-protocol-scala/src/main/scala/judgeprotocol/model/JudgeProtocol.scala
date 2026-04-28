package judgeprotocol.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SubmissionId(value: Long)

object SubmissionId:
  given Encoder[SubmissionId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[SubmissionId] = Decoder.decodeLong.emap { value =>
    if value < 1 then Left("Submission id is invalid.") else Right(SubmissionId(value))
  }

final case class ProblemSlug(value: String)

object ProblemSlug:
  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap { value =>
    val normalized = value.trim
    if normalized.isEmpty then Left("Problem slug is required.") else Right(ProblemSlug(normalized))
  }

final case class SubmissionSourceCode(value: String)

object SubmissionSourceCode:
  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Submission source code is required.")
    else if raw.length > 200000 then Left("Submission source code must be at most 200000 characters.")
    else Right(SubmissionSourceCode(raw))

  given Encoder[SubmissionSourceCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[SubmissionSourceCode] = Decoder.decodeString.emap(parse)

final case class ProblemTimeLimitMs(value: Int)

object ProblemTimeLimitMs:
  def parse(raw: Int): Either[String, ProblemTimeLimitMs] =
    if raw < 1 then Left("Problem time limit must be greater than 0.")
    else if raw > 600000 then Left("Problem time limit must be at most 600000 ms.")
    else Right(ProblemTimeLimitMs(raw))

  given Encoder[ProblemTimeLimitMs] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemTimeLimitMs] = Decoder.decodeInt.emap(parse)

final case class ProblemSpaceLimitMb(value: Int)

object ProblemSpaceLimitMb:
  def parse(raw: Int): Either[String, ProblemSpaceLimitMb] =
    if raw < 1 then Left("Problem space limit must be greater than 0.")
    else if raw > 65536 then Left("Problem space limit must be at most 65536 MB.")
    else Right(ProblemSpaceLimitMb(raw))

  given Encoder[ProblemSpaceLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemSpaceLimitMb] = Decoder.decodeInt.emap(parse)

final case class TestcaseName(value: String)

object TestcaseName:
  def parse(raw: String): Either[String, TestcaseName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Testcase name is required.")
    else if normalized.length > 120 then Left("Testcase name must be at most 120 characters.")
    else Right(TestcaseName(normalized))

  given Encoder[TestcaseName] = Encoder.encodeString.contramap(_.value)
  given Decoder[TestcaseName] = Decoder.decodeString.emap(parse)

final case class JudgerId(value: String)

object JudgerId:
  def parse(raw: String): Either[String, JudgerId] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Judger id is required.")
    else if normalized.length > 120 then Left("Judger id must be at most 120 characters.")
    else Right(JudgerId(normalized))

  given Encoder[JudgerId] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgerId] = Decoder.decodeString.emap(parse)

enum SubmissionLanguage:
  case Cpp17
  case Python3

object SubmissionLanguage:
  def render(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"

  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap {
    case "cpp17" => Right(SubmissionLanguage.Cpp17)
    case "python3" => Right(SubmissionLanguage.Python3)
    case other => Left(s"Unsupported submission language: $other")
  }

enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

object SubmissionStatus:
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

enum SubmissionVerdict:
  case Accepted
  case WrongAnswer
  case CompileError
  case RuntimeError
  case TimeLimitExceeded
  case SystemError

object SubmissionVerdict:
  def render(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  given Encoder[SubmissionVerdict] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionVerdict] = Decoder.decodeString.emap {
    case "accepted" => Right(SubmissionVerdict.Accepted)
    case "wrong_answer" => Right(SubmissionVerdict.WrongAnswer)
    case "compile_error" => Right(SubmissionVerdict.CompileError)
    case "runtime_error" => Right(SubmissionVerdict.RuntimeError)
    case "time_limit_exceeded" => Right(SubmissionVerdict.TimeLimitExceeded)
    case "system_error" => Right(SubmissionVerdict.SystemError)
    case other => Left(s"Unsupported submission verdict: $other")
  }

final case class RegisterJudgerRequest(
  preferredPrefix: JudgerId,
  host: String,
  processId: Option[String],
  supportedLanguages: List[SubmissionLanguage]
)

object RegisterJudgerRequest:
  given Encoder[RegisterJudgerRequest] = deriveEncoder[RegisterJudgerRequest]
  given Decoder[RegisterJudgerRequest] = deriveDecoder[RegisterJudgerRequest]

final case class RegisterJudgerResponse(
  judgerId: JudgerId,
  heartbeatIntervalMs: Long,
  heartbeatTimeoutMs: Long
)

object RegisterJudgerResponse:
  given Encoder[RegisterJudgerResponse] = deriveEncoder[RegisterJudgerResponse]
  given Decoder[RegisterJudgerResponse] = deriveDecoder[RegisterJudgerResponse]

final case class JudgerHeartbeatRequest()

object JudgerHeartbeatRequest:
  given Encoder[JudgerHeartbeatRequest] = deriveEncoder[JudgerHeartbeatRequest]
  given Decoder[JudgerHeartbeatRequest] = deriveDecoder[JudgerHeartbeatRequest]

final case class ClaimJudgeTaskRequest(judgerId: JudgerId)

object ClaimJudgeTaskRequest:
  given Encoder[ClaimJudgeTaskRequest] = deriveEncoder[ClaimJudgeTaskRequest]
  given Decoder[ClaimJudgeTaskRequest] = deriveDecoder[ClaimJudgeTaskRequest]

final case class JudgeTaskFileRef(
  path: String,
  sizeBytes: Long,
  sha256: String
)

object JudgeTaskFileRef:
  given Encoder[JudgeTaskFileRef] = deriveEncoder[JudgeTaskFileRef]
  given Decoder[JudgeTaskFileRef] = deriveDecoder[JudgeTaskFileRef]

final case class JudgeTaskTestcase(
  name: TestcaseName,
  input: JudgeTaskFileRef,
  expectedOutput: JudgeTaskFileRef
)

object JudgeTaskTestcase:
  given Encoder[JudgeTaskTestcase] = deriveEncoder[JudgeTaskTestcase]
  given Decoder[JudgeTaskTestcase] = deriveDecoder[JudgeTaskTestcase]

final case class JudgeTask(
  submissionId: SubmissionId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  timeLimitMs: ProblemTimeLimitMs,
  spaceLimitMb: ProblemSpaceLimitMb,
  problemDataVersion: String,
  testcases: List[JudgeTaskTestcase]
)

object JudgeTask:
  given Encoder[JudgeTask] = deriveEncoder[JudgeTask]
  given Decoder[JudgeTask] = deriveDecoder[JudgeTask]

final case class ReportJudgeResultRequest(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

object ReportJudgeResultRequest:
  given Encoder[ReportJudgeResultRequest] = deriveEncoder[ReportJudgeResultRequest]
  given Decoder[ReportJudgeResultRequest] = deriveDecoder[ReportJudgeResultRequest]
