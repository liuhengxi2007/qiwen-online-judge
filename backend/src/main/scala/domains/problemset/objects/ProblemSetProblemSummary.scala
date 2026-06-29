package domains.problemset.objects

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 题单中的题目摘要，包含题目身份和题单内位置。 */
final case class ProblemSetProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  position: Int
)

/** 提供题单题目摘要 JSON codec。 */
object ProblemSetProblemSummary:
  given Encoder[ProblemSetProblemSummary] = deriveEncoder[ProblemSetProblemSummary]
  given Decoder[ProblemSetProblemSummary] = deriveDecoder[ProblemSetProblemSummary]
