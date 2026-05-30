package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{ProblemSpaceLimitMb, ProblemTimeLimitMs}

final case class JudgeTaskLimits(
  timeMs: ProblemTimeLimitMs,
  memoryMb: ProblemSpaceLimitMb
)

object JudgeTaskLimits:
  given Encoder[JudgeTaskLimits] = deriveEncoder[JudgeTaskLimits]
  given Decoder[JudgeTaskLimits] = deriveDecoder[JudgeTaskLimits]
