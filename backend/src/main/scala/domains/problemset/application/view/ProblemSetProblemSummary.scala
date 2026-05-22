package domains.problemset.application.view

import domains.problemset.model.*

import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ProblemSetProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  position: Int
)

object ProblemSetProblemSummary:
  given Encoder[ProblemSetProblemSummary] = deriveEncoder[ProblemSetProblemSummary]
  given Decoder[ProblemSetProblemSummary] = deriveDecoder[ProblemSetProblemSummary]
