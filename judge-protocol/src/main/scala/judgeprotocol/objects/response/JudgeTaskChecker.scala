package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 测试点 checker 配置；builtin 使用 name，外部 checker 使用 source。 */
final case class JudgeTaskChecker(
  `type`: String,
  name: Option[String],
  source: Option[JudgeTaskFileRef]
)

/** 负责 checker 配置的协议编解码。 */
object JudgeTaskChecker:
  given Encoder[JudgeTaskChecker] = deriveEncoder[JudgeTaskChecker]
  given Decoder[JudgeTaskChecker] = deriveDecoder[JudgeTaskChecker]
