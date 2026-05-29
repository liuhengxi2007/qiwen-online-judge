package domains.problem.objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemReference(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle
)

object ProblemReference:
  given Encoder[ProblemReference] = deriveEncoder[ProblemReference]
  given Decoder[ProblemReference] = deriveDecoder[ProblemReference]
