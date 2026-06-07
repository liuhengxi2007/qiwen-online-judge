package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskHackConfig(
  enabled: Boolean,
  answerGeneration: String
)

object JudgeTaskHackConfig:
  val StandardAnswerGeneration: String = "standard"
  val NoAnswerGeneration: String = "none"

  val Disabled: JudgeTaskHackConfig =
    JudgeTaskHackConfig(enabled = false, answerGeneration = NoAnswerGeneration)

  val WithAnswerGenerator: JudgeTaskHackConfig =
    JudgeTaskHackConfig(enabled = true, answerGeneration = StandardAnswerGeneration)

  val WithoutAnswerGenerator: JudgeTaskHackConfig =
    JudgeTaskHackConfig(enabled = true, answerGeneration = NoAnswerGeneration)

  given Encoder[JudgeTaskHackConfig] = deriveEncoder[JudgeTaskHackConfig]
  given Decoder[JudgeTaskHackConfig] = deriveDecoder[JudgeTaskHackConfig]
