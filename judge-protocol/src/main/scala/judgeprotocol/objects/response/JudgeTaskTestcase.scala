package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskTestcase(
  index: Int,
  label: Option[String],
  scoreRatio: BigDecimal,
  limits: JudgeTaskLimits,
  checker: JudgeTaskChecker,
  input: JudgeTaskFileRef,
  answer: Option[JudgeTaskFileRef],
  strategyProvider: Option[JudgeTaskTool]
)

object JudgeTaskTestcase:
  given Encoder[JudgeTaskTestcase] = deriveEncoder[JudgeTaskTestcase]
  given Decoder[JudgeTaskTestcase] = deriveDecoder[JudgeTaskTestcase]
