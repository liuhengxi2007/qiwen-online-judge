package domains.contest.objects

import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 比赛题目摘要，包含赛内位置和别名，用于详情页与榜单结果。 */
final case class ContestProblemSummary(
  id: ProblemId,
  slug: ProblemSlug,
  title: ProblemTitle,
  position: Int,
  alias: ContestProblemAlias
)

/** 提供比赛题目摘要 JSON codec。 */
object ContestProblemSummary:
  given Encoder[ContestProblemSummary] = deriveEncoder[ContestProblemSummary]
  given Decoder[ContestProblemSummary] = deriveDecoder[ContestProblemSummary]
