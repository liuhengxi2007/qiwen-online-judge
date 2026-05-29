package domains.problem.objects.response

import domains.problem.objects.ProblemReference
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ResolveProblemReferenceResponse(
  problem: Option[ProblemReference]
)

object ResolveProblemReferenceResponse:
  given Encoder[ResolveProblemReferenceResponse] = deriveEncoder[ResolveProblemReferenceResponse]
  given Decoder[ResolveProblemReferenceResponse] = deriveDecoder[ResolveProblemReferenceResponse]
