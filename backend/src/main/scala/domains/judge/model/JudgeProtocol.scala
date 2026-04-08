package domains.judge.model

import domains.problem.model.ProblemSlug
import domains.submission.model.{SubmissionId, SubmissionLanguage, SubmissionStatus, SubmissionVerdict}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgerName(value: String)

object JudgerName:
  def parse(raw: String): Either[String, JudgerName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Judger name is required.")
    else if normalized.length > 120 then Left("Judger name must be at most 120 characters.")
    else Right(JudgerName(normalized))

  def unsafe(raw: String): JudgerName =
    parse(raw).fold(message => throw IllegalStateException(s"Invalid judger name: $message"), identity)

  given Encoder[JudgerName] = Encoder.encodeString.contramap(_.value)
  given Decoder[JudgerName] = Decoder.decodeString.emap(parse)

final case class ClaimJudgeTaskRequest(
  judgerName: JudgerName
)

object ClaimJudgeTaskRequest:
  given Encoder[ClaimJudgeTaskRequest] = deriveEncoder[ClaimJudgeTaskRequest]
  given Decoder[ClaimJudgeTaskRequest] = deriveDecoder[ClaimJudgeTaskRequest]

final case class JudgeTaskTestcase(
  name: String,
  inputBase64: String,
  expectedOutputBase64: String
)

object JudgeTaskTestcase:
  given Encoder[JudgeTaskTestcase] = deriveEncoder[JudgeTaskTestcase]
  given Decoder[JudgeTaskTestcase] = deriveDecoder[JudgeTaskTestcase]

final case class JudgeTask(
  submissionId: SubmissionId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: String,
  timeLimitMs: Int,
  spaceLimitMb: Int,
  testcases: List[JudgeTaskTestcase]
)

object JudgeTask:
  given Encoder[JudgeTask] = deriveEncoder[JudgeTask]
  given Decoder[JudgeTask] = deriveDecoder[JudgeTask]

final case class ReportJudgeResultRequest(
  status: SubmissionStatus,
  verdict: Option[SubmissionVerdict],
  judgeMessage: Option[String]
)

object ReportJudgeResultRequest:
  given Encoder[ReportJudgeResultRequest] = deriveEncoder[ReportJudgeResultRequest]
  given Decoder[ReportJudgeResultRequest] = deriveDecoder[ReportJudgeResultRequest]
