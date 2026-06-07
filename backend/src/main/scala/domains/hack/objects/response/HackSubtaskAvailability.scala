package domains.hack.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class HackSubtaskAvailability(
  subtaskIndex: Int,
  canHack: Boolean,
  reason: Option[String]
)

object HackSubtaskAvailability:
  given Encoder[HackSubtaskAvailability] = deriveEncoder[HackSubtaskAvailability]
  given Decoder[HackSubtaskAvailability] = deriveDecoder[HackSubtaskAvailability]
