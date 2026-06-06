package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeResultMetrics(
  score: BigDecimal,
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

object JudgeResultMetrics:
  val failed: JudgeResultMetrics = JudgeResultMetrics(BigDecimal(0), None, None)

  given Encoder[JudgeResultMetrics] = deriveEncoder[JudgeResultMetrics]
  given Decoder[JudgeResultMetrics] = deriveDecoder[JudgeResultMetrics]
