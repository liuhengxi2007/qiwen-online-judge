package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.SubmissionVerdict

final case class JudgeTestcaseResult(
  name: String,
  score: BigDecimal,
  verdict: SubmissionVerdict,
  message: Option[String],
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

object JudgeTestcaseResult:
  given Encoder[JudgeTestcaseResult] = deriveEncoder[JudgeTestcaseResult]
  given Decoder[JudgeTestcaseResult] = deriveDecoder[JudgeTestcaseResult]
