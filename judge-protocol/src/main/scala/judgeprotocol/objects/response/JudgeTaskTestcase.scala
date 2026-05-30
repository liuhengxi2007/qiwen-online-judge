package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.TestcaseName

final case class JudgeTaskTestcase(
  name: TestcaseName,
  scoreRatio: BigDecimal,
  limits: JudgeTaskLimits,
  checker: JudgeTaskChecker,
  input: Option[JudgeTaskFileRef],
  answer: JudgeTaskFileRef
)

object JudgeTaskTestcase:
  given Encoder[JudgeTaskTestcase] = deriveEncoder[JudgeTaskTestcase]
  given Decoder[JudgeTaskTestcase] = deriveDecoder[JudgeTaskTestcase]
