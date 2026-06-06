package domains.hack.objects.request

import domains.submission.objects.SubmissionId
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class CreateHackRequest(
  targetSubmissionId: SubmissionId,
  subtaskIndex: Int,
  input: String,
  strategyProviderSource: Option[String]
)

object CreateHackRequest:
  given Encoder[CreateHackRequest] = deriveEncoder[CreateHackRequest]
  given Decoder[CreateHackRequest] = deriveDecoder[CreateHackRequest]
