package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeResult(
  score: BigDecimal,
  verdict: SubmissionVerdict,
  message: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long],
  subtasks: List[JudgeSubtaskResult]
)

object JudgeResult:
  given Encoder[JudgeResult] = deriveEncoder[JudgeResult]
  given Decoder[JudgeResult] = deriveDecoder[JudgeResult]
