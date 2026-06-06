package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeResultMetrics(
  score: BigDecimal,
  timeUsedMs: Option[Long],
  memoryUsedKb: Option[Long]
)

object JudgeResultMetrics:
  given Encoder[JudgeResultMetrics] = deriveEncoder[JudgeResultMetrics]
  given Decoder[JudgeResultMetrics] = deriveDecoder[JudgeResultMetrics]
