package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskSubtask(
  index: Int,
  label: Option[String],
  scoreRatio: BigDecimal,
  mode: JudgeTaskMode,
  validator: Option[JudgeTaskTool],
  aggregation: JudgeTaskAggregation,
  testcases: List[JudgeTaskTestcase]
)

object JudgeTaskSubtask:
  given Encoder[JudgeTaskSubtask] = deriveEncoder[JudgeTaskSubtask]
  given Decoder[JudgeTaskSubtask] = deriveDecoder[JudgeTaskSubtask]
