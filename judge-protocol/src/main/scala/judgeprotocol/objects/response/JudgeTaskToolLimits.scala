package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{TestcaseMemoryLimitMb, TestcaseTimeLimitMs}

final case class JudgeTaskToolLimits(
  realTimeMs: TestcaseTimeLimitMs,
  memoryMb: TestcaseMemoryLimitMb
)

object JudgeTaskToolLimits:
  given Encoder[JudgeTaskToolLimits] = deriveEncoder[JudgeTaskToolLimits]
  given Decoder[JudgeTaskToolLimits] = deriveDecoder[JudgeTaskToolLimits]
