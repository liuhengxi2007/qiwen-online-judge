package domains.problemset.model

import domains.shared.access.ResourceAccessPolicy
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UpdateProblemSetRequest(
  title: ProblemSetTitle,
  description: ProblemSetDescription,
  accessPolicy: ResourceAccessPolicy
)

object UpdateProblemSetRequest:
  given Encoder[UpdateProblemSetRequest] = deriveEncoder[UpdateProblemSetRequest]
  given Decoder[UpdateProblemSetRequest] = deriveDecoder[UpdateProblemSetRequest]
