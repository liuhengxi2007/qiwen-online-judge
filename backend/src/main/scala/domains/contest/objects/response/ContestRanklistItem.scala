package domains.contest.objects.response

import domains.contest.objects.{ContestPenaltyMillis, ContestRank, ContestScore}
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 比赛榜单单行响应，包含用户总分、罚时和逐题结果。 */
final case class ContestRanklistItem(
  rank: ContestRank,
  user: UserIdentity,
  totalScore: ContestScore,
  penaltyMillis: ContestPenaltyMillis,
  problemResults: List[ContestRanklistProblemResult]
)

/** 提供比赛榜单单行 JSON codec。 */
object ContestRanklistItem:
  given Encoder[ContestRanklistItem] = deriveEncoder[ContestRanklistItem]
  given Decoder[ContestRanklistItem] = deriveDecoder[ContestRanklistItem]
