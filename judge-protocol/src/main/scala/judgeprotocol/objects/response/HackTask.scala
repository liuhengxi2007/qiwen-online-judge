package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** backend 下发给 judger 的 hack 尝试任务，包含目标任务快照、输入和旧结果。 */
final case class HackTask(
  hackId: Long,
  targetTask: JudgeTask,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String],
  oldResult: JudgeResult
)

/** 负责 hack 任务的协议编解码。 */
object HackTask:
  given Encoder[HackTask] = deriveEncoder[HackTask]
  given Decoder[HackTask] = deriveDecoder[HackTask]
