package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{TestcaseMemoryLimitMb, TestcaseTimeLimitMs}

final case class JudgeTaskLimits(
  timeMs: TestcaseTimeLimitMs,
  memoryMb: TestcaseMemoryLimitMb
)

object JudgeTaskLimits:
  given Encoder[JudgeTaskLimits] = deriveEncoder[JudgeTaskLimits]
  given Decoder[JudgeTaskLimits] = deriveDecoder[JudgeTaskLimits]
