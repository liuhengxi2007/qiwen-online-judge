package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import judgeprotocol.objects.{TestcaseMemoryLimitMb, TestcaseTimeLimitMs}

/** 单个测试点运行选手程序时使用的资源限制。 */
final case class JudgeTaskLimits(
  timeMs: TestcaseTimeLimitMs,
  memoryMb: TestcaseMemoryLimitMb
)

/** 负责测试点资源限制的协议编解码，校验委托给内部值对象。 */
object JudgeTaskLimits:
  given Encoder[JudgeTaskLimits] = deriveEncoder[JudgeTaskLimits]
  given Decoder[JudgeTaskLimits] = deriveDecoder[JudgeTaskLimits]
