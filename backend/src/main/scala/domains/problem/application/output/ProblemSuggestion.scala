package domains.problem.application.output

import domains.problem.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemSuggestion(
  slug: ProblemSlug,
  title: ProblemTitle
)

object ProblemSuggestion:
  given Encoder[ProblemSuggestion] = deriveEncoder[ProblemSuggestion]
  given Decoder[ProblemSuggestion] = deriveDecoder[ProblemSuggestion]
