package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class HackTask(
  hackId: Long,
  targetTask: JudgeTask,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String],
  oldResult: JudgeResult
)

object HackTask:
  given Encoder[HackTask] = deriveEncoder[HackTask]
  given Decoder[HackTask] = deriveDecoder[HackTask]
