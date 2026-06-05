package domains.contest.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ContestProblemAttachWarningResponse(
  shouldWarn: Boolean
)

object ContestProblemAttachWarningResponse:
  given Encoder[ContestProblemAttachWarningResponse] = deriveEncoder[ContestProblemAttachWarningResponse]
  given Decoder[ContestProblemAttachWarningResponse] = deriveDecoder[ContestProblemAttachWarningResponse]
