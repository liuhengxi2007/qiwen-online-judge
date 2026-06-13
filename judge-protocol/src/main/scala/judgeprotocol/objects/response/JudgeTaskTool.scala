package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** checker、validator、interactor 或 strategy provider 的源文件与可选资源限制。 */
final case class JudgeTaskTool(
  source: JudgeTaskFileRef,
  limits: Option[JudgeTaskToolLimits]
)

/** 负责判题工具引用的协议编解码。 */
object JudgeTaskTool:
  given Encoder[JudgeTaskTool] = deriveEncoder[JudgeTaskTool]
  given Decoder[JudgeTaskTool] = deriveDecoder[JudgeTaskTool]
