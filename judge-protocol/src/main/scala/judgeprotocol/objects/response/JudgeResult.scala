package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeResult(
  baseResult: JudgeResultMetrics,
  worstResult: JudgeResultMetrics,
  verdict: SubmissionVerdict,
  reason: Option[JudgeFailureReason],
  subtasks: List[JudgeSubtaskResult]
)

object JudgeResult:
  given Encoder[JudgeResult] = deriveEncoder[JudgeResult]
  given Decoder[JudgeResult] = deriveDecoder[JudgeResult]
