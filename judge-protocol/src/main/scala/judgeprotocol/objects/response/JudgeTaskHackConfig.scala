package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 子任务是否允许 hack，以及是否需要由标准程序生成答案。 */
final case class JudgeTaskHackConfig(
  enabled: Boolean,
  answerGeneration: String
)

/** 提供 hack 配置的协议常量和编解码。 */
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
