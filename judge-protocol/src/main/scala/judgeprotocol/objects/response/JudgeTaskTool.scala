package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class JudgeTaskTool(
  source: JudgeTaskFileRef,
  limits: Option[JudgeTaskToolLimits]
)

object JudgeTaskTool:
  given Encoder[JudgeTaskTool] = deriveEncoder[JudgeTaskTool]
  given Decoder[JudgeTaskTool] = deriveDecoder[JudgeTaskTool]
