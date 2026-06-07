package domains.hack.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SubmissionHackAvailability(
  subtasks: List[HackSubtaskAvailability]
)

object SubmissionHackAvailability:
  given Encoder[SubmissionHackAvailability] = deriveEncoder[SubmissionHackAvailability]
  given Decoder[SubmissionHackAvailability] = deriveDecoder[SubmissionHackAvailability]
