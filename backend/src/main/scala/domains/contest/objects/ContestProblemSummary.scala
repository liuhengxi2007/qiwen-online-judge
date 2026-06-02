package domains.contest.objects

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ContestProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  position: Int,
  alias: ContestProblemAlias
)

object ContestProblemSummary:
  given Encoder[ContestProblemSummary] = deriveEncoder[ContestProblemSummary]
  given Decoder[ContestProblemSummary] = deriveDecoder[ContestProblemSummary]
