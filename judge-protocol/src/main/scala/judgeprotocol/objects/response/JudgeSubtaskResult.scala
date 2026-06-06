package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeSubtaskResult(
  index: Int,
  label: Option[String],
  baseResult: JudgeResultMetrics,
  worstResult: JudgeResultMetrics,
  verdict: SubmissionVerdict,
  reason: Option[JudgeFailureReason],
  testcases: List[JudgeTestcaseResult]
)

object JudgeSubtaskResult:
  given Encoder[JudgeSubtaskResult] = deriveEncoder[JudgeSubtaskResult]
  given Decoder[JudgeSubtaskResult] = deriveDecoder[JudgeSubtaskResult]
