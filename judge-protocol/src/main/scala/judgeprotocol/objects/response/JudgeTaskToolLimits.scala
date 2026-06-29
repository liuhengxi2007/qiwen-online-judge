package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{TestcaseMemoryLimitMb, TestcaseTimeLimitMs}

/** 判题工具自身运行时的资源限制，独立于选手程序限制。 */
final case class JudgeTaskToolLimits(
  timeMs: TestcaseTimeLimitMs,
  memoryMb: TestcaseMemoryLimitMb
)

/** 负责工具资源限制的协议编解码。 */
object JudgeTaskToolLimits:
  given Encoder[JudgeTaskToolLimits] = deriveEncoder[JudgeTaskToolLimits]
  given Decoder[JudgeTaskToolLimits] = deriveDecoder[JudgeTaskToolLimits]
