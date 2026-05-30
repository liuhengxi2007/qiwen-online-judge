package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskSubtask(
  name: String,
  scoreRatio: BigDecimal,
  aggregation: JudgeTaskAggregation,
  testcases: List[JudgeTaskTestcase]
)

object JudgeTaskSubtask:
  given Encoder[JudgeTaskSubtask] = deriveEncoder[JudgeTaskSubtask]
  given Decoder[JudgeTaskSubtask] = deriveDecoder[JudgeTaskSubtask]
