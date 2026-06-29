package domains.hack.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 单个子任务的 hack 可用性；reason 为前端可翻译的不可用原因码。 */
final case class HackSubtaskAvailability(
  subtaskIndex: Int,
  canHack: Boolean,
  reason: Option[String]
)

/** HackSubtaskAvailability 的 JSON 编解码器。 */
object HackSubtaskAvailability:
  given Encoder[HackSubtaskAvailability] = deriveEncoder[HackSubtaskAvailability]
  given Decoder[HackSubtaskAvailability] = deriveDecoder[HackSubtaskAvailability]
